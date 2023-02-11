import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.DecimalFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Scraper {
    private static final Logger logger = LogManager.getLogger(Scraper.class);
    private static final String CAPTAIN_COASTER_BASE_RANKING_URL = "https://captaincoaster.com/fr/ranking/?filters%5Bcontinent%5D=CONTINENT&filters%5Bcountry%5D=COUNTRY&filters%5BmaterialType%5D=&filters%5BseatingType%5D=&filters%5Bmodel%5D=&filters%5Bmanufacturer%5D=&filters%5BopeningDate%5D=&page=1";
    private static final String CAPTAIN_COASTER_BASE_URL_PAGE = "https://captaincoaster.com/fr/ranking/?filters%5Bcontinent%5D=CONTINENT&filters%5Bcountry%5D=COUNTRY&filters%5BmaterialType%5D=&filters%5BseatingType%5D=&filters%5Bmodel%5D=&filters%5Bmanufacturer%5D=&filters%5BopeningDate%5D=&page=";
    public static final int MIN_ATTRACTIONS = 5;
    public static final int MAX_RESULT = 10;
    private static int MAX_PAGE = 1;
    private static Set<String> parks = new HashSet<>();

    private static Map<String, Double> mapParksScores = new HashMap<>();

    private static final Continent CONTINENT = Continent.Europe;
    private static final Country COUNTRY = Country.Country;

    public static void main(String[] args) {
        WebDriver driver = new ChromeDriver();
        String baseUrl = StringUtils.replace(StringUtils.replace(CAPTAIN_COASTER_BASE_RANKING_URL, "CONTINENT", CONTINENT.getContinentCode()), "COUNTRY", COUNTRY.getCountryCode());
        logger.info("Base url used {}", baseUrl);
        driver.get(baseUrl);
        getMaxPage(driver);
        getUniqueParks(driver);
        logger.info("Retrieved {} unique parks with parameter continent {} and country {}", parks.size(), CONTINENT.name(), COUNTRY.name());
        getAverageScorePerPark(driver);
        Stream<Map.Entry<String,Double>> sorted = mapParksScores.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()));
        AtomicInteger i = new AtomicInteger(1);
        sorted.limit(MAX_RESULT).forEach(entry -> logger.info("Top {} : {} with {} %", i.getAndIncrement(), entry.getKey(), entry.getValue()));
    }

    private static void getAverageScorePerPark(WebDriver driver) {
        for(String park : parks){
            driver.get(park);
            WebElement attractionsList = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.media-list")));
            List<WebElement> attractions = attractionsList.findElements(By.cssSelector("li.border-bottom-success div.media-body h4"));
            if(attractions.size() > MIN_ATTRACTIONS){
                List<Double> scores = new ArrayList<>();
                for(WebElement attraction : attractions){
                    String rawScore = attraction.getText();
                    if(rawScore.contains("de") && rawScore.contains("%")){
                        String score = StringUtils.substringBetween(rawScore, "de", "%").trim();
                        scores.add(Double.parseDouble(score.replace(",", ".")));
                    }
                }
                OptionalDouble averageScore = scores.stream().mapToDouble(a -> a).average();
                if(averageScore.isPresent()){
                    DecimalFormat df = new DecimalFormat("##.##");
                    String trimmedScore = df.format(averageScore.getAsDouble());
                    logger.info("Average score for park {} is {} %", park, trimmedScore);
                    mapParksScores.put(park, Double.valueOf(trimmedScore));
                }
            }
        }
    }

    private static void getUniqueParks(WebDriver driver) {
        for(int i = 1; i <= MAX_PAGE; i++){
            String urlPark = StringUtils.replace(StringUtils.replace(CAPTAIN_COASTER_BASE_URL_PAGE, "CONTINENT", CONTINENT.getContinentCode()), "COUNTRY", COUNTRY.getCountryCode());
            driver.get(urlPark+i);
            List<WebElement> scrapedParks = new WebDriverWait(driver, Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.media-body ul li a.text-muted")));
            for(WebElement park : scrapedParks){
                parks.add(park.getAttribute("href"));
            }
        }
    }

    private static void getMaxPage(WebDriver driver) {
        List<WebElement> pageLinks = driver.findElements(By.className("page-link"));
        for(WebElement link : pageLinks){
            if(link.getText().matches("\\d+")){
                int page = Integer.parseInt(link.getText());
                if(page > MAX_PAGE){
                    MAX_PAGE = page;
                }
            }
        }
    }
}

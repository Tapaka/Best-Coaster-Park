public enum Continent {
    Monde(""),
    Amérique("2"),
    Asie("3"),
    Europe("1"),
    Océanie("5"),
    Afrique("4");

    private final String continentCode;

    private Continent(String continentCode) {
        this.continentCode = continentCode;
    }

    public String getContinentCode() {
        return continentCode;
    }
}

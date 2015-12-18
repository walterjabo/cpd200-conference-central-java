
    @ApiMethod(
            name = "filterPlayground",
            path = "filterPlayground",
            httpMethod = HttpMethod.POST
    )
    public List<Conference> filterPlayground() {
        Query<Conference> query = ofy().load().type(Conference.class);

        // Filter on city
        // query = query.filter("city =", "Paris");

        /*
        TODO
        add 2 filters:
        1: city equals to Chicago
        2: topic equals "Medical Innovations"
        */

        return query.list();
    }

# Java_REST_Project

# Introducing New Features 

The two new features will be dependent on an additional property, rating, in the movie node. The movie rating will be represented using the float data type with one decimal place to display a rating out of ten. The following features assume that movie rating is a parameter in the PUT addMovie endpoint and can be set by the endpoint.  
Feature 1: getHighestRatedMovie  
GET /api/v1/getHighestRatedMovie 
o	Description: This endpoint is to retrieve the movie node with the highest rating out of all the movies in the database. 
o	Body Parameters: 
▪ None o Body Example:
{ 
} 
 
o	Response: 
▪	highestRatedMovies: List of JSON objects: 
▪	movieId: String 
▪	name: String 
▪	actors: List of Strings 
▪	rating: Float 
o	Response Body Example
{ 
    "highestRatedMovies": [ 
        { 
            "actors": [ 
                "nm8911231", 
                "nm1991341", 
                "nm0000102" 
            ], 
            "name": "A Few Good Men", 
            "rating": 9.2, 
            "movieId": "nm1111891" 
        }, 
        { 
            "actors": [ 
                "nm9001231", 
                "nm8001341", 
                "nm7000102" 
            ], 
            "name": "The Matrix", 
            "rating": 9.2, 
            "movieId": "nm0001891" 
        }, 
 
    ] 
	} 	 
o	Expected Response: 
▪	200 OK – For a successful computation and comparison 
▪	400 BAD REQUEST – If the request body is improperly formatted 
▪	404 NOT FOUND – If there are no movies in the database  
▪	500 INTERNAL SERVER ERROR – If save or add was unsuccessful (Java Exception 
Thrown) 
o	Edge Cases: 
▪	If there are multiple movies that have the highest rating, return all of them 
▪	If the database has no movies, or if the movie nodes do not have the movie rating parameter filled, return a status of 404 and nothing else 
▪	If there is only one movie node, it is by default the highest rated movie 
Feature 2: getSortedMoviesForActor 
GET /api/v1/getSortedMoviesForActor 
o	Description: This endpoint is to retrieve a list of names and ratings of movies for an actor, sorted from highest rating to lowest.  
o	Body Parameters: 
▪	actorId: String o Body Example:
{ 
"actorId": "nm1001231" 
} 
 o Response: 
▪	actorId: String 
▪	movies: List of JSON objects: 
▪	name: String 
▪	rating: Float 
o	Response Body Example: 
{ 
    "actorId": "nm1123567", 
    "movies": [ 
        { 
            "name": "A Few Good Men", 
            "rating": 9.2 
        }, 
        { 
            "name": "Top Gun", 
            "rating": 8.9 
        }, 
	 	 … 
    ] 
} 
 
 o Expected Response: 
▪	200 OK – For successfully retrieving the movies for an actor 
▪	400 BAD REQUEST – If the request body is improperly formatted or missing required information 
▪	404 NOT FOUND – If there are no actors in the database that exists with the given actorId 
▪	500 INTERNAL SERVER ERROR – If save or add was unsuccessful (Java Exception 
Thrown) 
o Edge Cases: 
▪	If the actor has not acted in any movies or none of the movies have the “rating” property, return an empty list for movies 
▪	If the movie nodes associated to the actor has no rating, do not add to the list 
▪	If the actor has multiple movies with the same rating, then the order in which they appear does not matter 
 
 
 

/*
 * EECS3311, Project
 */
package ca.yorku.eecs;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.*;
import org.json.*;
import org.neo4j.driver.v1.*;

public class Neo4jMovies {
	private Driver driver;
	private String uriDb;
	
	/**
	 * Neo4jMovies Constructor.
	 * Used to interact and query the Neo4j database.
	 */
	public Neo4jMovies() {
		uriDb = "bolt://localhost:7687";
		Config config = Config.builder().withoutEncryption().build();
		driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j","123456"), config);
	}
	
	// ========================================================================
	// PUT Methods
	// ========================================================================
	
	/**
	 * Creates and adds an actor node to database.
	 * @param name
	 * @param actorId 
	 */
	public void insertActor(String name, String actorId) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run("MERGE (a:actor {Name: $x, id: $y})", 
					parameters("x", name, "y", actorId)));
			session.close();
		}
	}
	
	/**
	 * Creates and adds a movie node to database.
	 * @param name
	 * @param movieId
	 * @param rating (optional parameter) 
	 */
	public void insertMovie(String name, String movieId, Double rating) {
		try (Session session = driver.session()){
			if (rating == -1.0) {
				// no rating parameter provided in request
				session.writeTransaction(tx -> tx.run("MERGE (m:movie {Name: $x, id: $y})", 
						parameters("x", name, "y", movieId)));				
			}
			else {
				session.writeTransaction(tx -> tx.run("MERGE (m:movie {Name: $x, id: $y, rating: $z})", 
						parameters("x", name, "y", movieId, "z", rating)));				
			}
			session.close();
		}
	}
	
	/**
	 * Creates an ACTED_IN relationship from the actor to the movie.
	 * @param actorId
	 * @param movieId
	 */
	public void insertRelationship(String actorId, String movieId) {
		try (Session session = driver.session()){
			session.writeTransaction(tx -> tx.run(
					"MATCH (a:actor), (m:movie)\n"
					+ "WHERE a.id = $x AND m.id = $y \n"
					+ "CREATE (a)-[r:ACTED_IN]->(m)\n"
					+ "RETURN type(r);", 
					parameters("x", actorId, "y", movieId)));
			session.close();
		}
	}
	
	// ========================================================================
	// GET Methods
	// ========================================================================
	/**
	 * Returns a JSONObject containing the actor's name, id, and a JSONArray
	 * with the movie ids of movies the actor has acted in
	 * @param actorId
	 * @return a JSONObject
	 * @throws JSONException
	 */
	public JSONObject getActor(String actorId) throws JSONException {
		// create JSON object that will be sent back to client
		JSONObject obj = new JSONObject();
		
		// get actor name
		try (Session session = driver.session()){
			// add actor id to JSON object
			obj.put("actorId", actorId);

			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (a:actor{id: $x})\n"
					+ "RETURN a.Name;", 
					parameters("x", actorId)));
			
			// get actor name
			Record record = result.list().get(0);			
			String name = record.get("a.Name").asString();
			
			// add actor name to JSON object
			obj.put("name", name);
			
			session.close();
		}
		
		// get movies the actor has acted in
		try (Session session = driver.session()){
			// add actor id to JSON object
			obj.put("actorId", actorId);

			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (a:actor{id: $x}), (m:movie)\n"
					+ "WHERE (a)-[:ACTED_IN]->(m)\n"
					+ "RETURN collect(m.id) as movieIds;", 
					parameters("x", actorId)));
			
			Record record = result.list().get(0);			
			
			// convert the list of movieIds into a JSON representation
			JSONArray list = new JSONArray();
			for (Value value : record.get("movieIds").values()) {
				list.put(value.asString());
			}
			
			// add list of movieIds to JSON object
			obj.put("movies", list);
			
			session.close();
		}
		
		return obj;
	}
	
	/**
	 * Returns a JSONObject containing the movie's name, id, and a JSONArray
	 * with the actor ids of actor's who act in the movie. 
	 * @param movieId
	 * @return a JSONObject
	 * @throws JSONException
	 */
	public JSONObject getMovie(String movieId) throws JSONException {
		// create JSON object that will be sent back to client
		JSONObject obj = new JSONObject();
		
		// get the movie name 
		try (Session session = driver.session()){
			// add movie id to JSON object
			obj.put("movieId", movieId);
			
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (m:movie{id: $x})\n"
					+ "RETURN m.Name", 
					parameters("x", movieId)));
			
			// get movie name
			Record record = result.list().get(0);			
			String name = record.get("m.Name").asString();
			
			// add movie name to JSON object
			obj.put("name", name);
			
			session.close();
		}
		
		// get the ids of all actors who act in the movie
		try (Session session = driver.session()){

			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (m:movie{id: $x}), (a:actor)\n"
					+ "WHERE (a)-[:ACTED_IN]->(m)\n"
					+ "RETURN collect(a.id) as actorIds", 
					parameters("x", movieId)));

			Record record = result.list().get(0);			
					
			// convert the list of actorIds into a JSON representation
			JSONArray list = new JSONArray();
			
			// add actorIds to JSON array
			for (Value value : record.get("actorIds").values()) {
				list.put(value.asString());
			}
			
			// add list of actorIds to JSON object
			obj.put("actors", list);
			
			session.close();
		}
		
		return obj;
	}

	/**
	 * Returns a JSONObject containing the bacon number for the provided actorId
	 * @param actorId
	 * @return a JSONObject
	 * @throws JSONException
	 */
	public JSONObject getBaconNumber(String actorId) throws JSONException {
		// create JSON object that will be sent back to client
		JSONObject obj = new JSONObject();
		
		/*
		 * Assumption: there is always a node with id nm0000102 that is 
		 * supposed to be Kevin Bacon, this node will not be renamed or
		 * deleted
		 */
		// check if actorId is Kevin Bacon's actorId
		if (actorId.equals("nm0000102")) {
			// actorId matches Kevin Bacon's id, and Kevin Bacon's bacon path is a list with just his actorId
			obj.put("baconNumber", 0);
			return obj;
		}
		
		// get the bacon number 
		try (Session session = driver.session()){
			
			// get number of movie nodes between the shortest path from Kevin Bacon and specified actor
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH p=shortestPath((actor:actor{id: $x})-[*]-(bacon:actor{Name:\"Kevin Bacon\"}))\r\n"
					+ "UNWIND nodes(p) AS pList\r\n"
					+ "WITH pList\r\n"
					+ "WHERE 'movie' IN labels(pList)\r\n"
					+ "RETURN count(pList) AS baconNumber", 
					parameters("x", actorId)));
			
			Record record = result.list().get(0);
			int baconNumber = record.get("baconNumber").asInt();
			
			// check if there is a path to Kevin Bacon
			if (baconNumber == 0) {
				// no path, no JSON object to return
				obj = null;
				return obj;
			}
			
			// add bacon number to JSON object
			obj.put("baconNumber", baconNumber);
			
			session.close();
		}
		
		return obj;
	}
	
	/**
	 * Returns a JSONObject containing a JSONArray with the shortest path
	 * from the actor to Kevin Bacon. The JSONArray will contain a list
	 * of interchaning actor ids and movie ids beginning with the inputted
	 * actorId and ending with Kevin Bacon's actorId, "nm0000102" 
	 * @param actorId
	 * @return
	 * @throws JSONException
	 */
	public JSONObject getBaconPath(String actorId) throws JSONException {

		// create JSON object that will be sent back to client
		JSONObject obj = new JSONObject();
		
		/*
		 * Assumption: there is always a node with id nm0000102 that is 
		 * supposed to be Kevin Bacon, this node will not be renamed or
		 * deleted
		 */
		// check if actorId is Kevin Bacon's actorId
		if (actorId.equals("nm0000102")) {
			// actorId matches Kevin Bacon's id, and Kevin Bacon's bacon path is a list with just his actorId
			JSONArray list = new JSONArray();
			list.put(actorId);
			obj.put("baconPath", list);
			return obj;
		}
		
		/*
		 * get the bacon path from given actor to Kevin Bacon
		 * the path is reprsented as a list which interchanges actors and movies beginning with inputted actorId and
		 * ending with Kevin Bacon's actorId
		 */
		try (Session session = driver.session()){
			
			// get path from Kevin Bacon and specified actor
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH p=shortestPath((actor:actor {id: $x})-[*]-(bacon:actor {Name: \"Kevin Bacon\"}))\n"
					+ "UNWIND nodes(p) as n\n"
					+ "RETURN n", 
					parameters("x", actorId)));
			
			JSONArray baconPath = new JSONArray();

			List<Record> records = result.list();
			
			// no path
			if (records.isEmpty()) {
				// no JSON object to return
				obj = null;
				return obj;
			}
			
			// extract actorId or movieId and add to baconPath
			for (Record record : records) {
				Value value = record.values().get(0);
				
				String id = "";
				
				if (!(value.get("id").isNull())) {
					id = value.get("id").asString();
				}

				baconPath.put(id);
			}
			
			// add bacon path to JSON object
			obj.put("baconPath", baconPath);
			
			session.close();
		}
		
		return obj;
	}
	
	/**
	 * Returns a JSONObject containing a JSONArray which contains a list of 
	 * the highest rated movies. This is so that multiple movies that share
	 * the highest rating in the database will be returned instead of just one. 
	 * @return JSONObject
	 * @throws JSONException
	 */
	public JSONObject getHighestRatedMovie() throws JSONException {
		// create JSON object that will be sent back to client
		JSONObject obj = new JSONObject();
		
		// create JSON array that will hold the movies
		JSONArray highestRatedMovies = new JSONArray();

		try (Session session = driver.session()){
			
			// get highest rated movies from database
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (m:movie)\n"
					+ "WITH max(m.rating) as maxRating\n"
					+ "MATCH (m:movie) WHERE m.rating = maxRating\n"
					+ "RETURN m"));
			
			List<Record> records = result.list();
			
			// no movie ratings
			if (records.isEmpty()) {
				// no JSON obj to return
				obj = null;
				return obj;
			}
			
			// extract movieId, movie name, movie rating, and actors from record
			// and put them in a JSON object
			for (Record record : records) {
				Value value = record.values().get(0);
				String movieId = value.get("id").asString();
				
				// call getMovie
				JSONObject movieEntry = getMovie(movieId);
				
				// add movie rating to movieEntry
				Double movieRating = value.get("rating").asDouble();
				movieEntry.put("rating", movieRating);
				highestRatedMovies.put(movieEntry);
			}
			
			session.close();
		}
		
		obj.put("highestRatedMovies", highestRatedMovies);
		return obj;
	}
	
	/**
	 * Returns a JSONObject containing the actor's id and a JSONArray containing
	 * a list of movie name's along with its rating sorted from the highest rating to
	 * lowest.
	 * @param actorId
	 * @return JSON Object
	 * @throws JSONException
	 */
	public JSONObject getSortedMoviesForActor(String actorId) throws JSONException {
		// create JSON array that will be sent back to client
		JSONObject obj = new JSONObject();

		try (Session session = driver.session()){
			obj.put("actorId", actorId);
			
			// get list of movies (names, rating) actor has acted in, 
			// sorted in descending order based on movie rating
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (a:actor{id: $x}), (m:movie)\r\n"
					+ "WHERE (a)-[:ACTED_IN]->(m)\r\n"
					+ "RETURN m.Name AS name, m.rating AS rating\r\n"
					+ "ORDER BY (m.rating) DESC", 
					parameters("x", actorId)));
			
			List<Record> records = result.list();
			
			JSONArray movies = new JSONArray();
			// extract movieId, movie name, movie rating, and actors from record
			// and put them in a JSON object
			for (Record record : records) {
				JSONObject movieEntry = new JSONObject();
				
				String movieName = record.get("name").asString();
				
				// check for rating property in movie node
				if (record.get("rating").isNull()) {
					// move on to next movie node
					continue;
				}
				
				Double movieRating = record.get("rating").asDouble();

				// add movie name and movie rating to movieEntry
				movieEntry.put("name", movieName);
				movieEntry.put("rating", movieRating);
				movies.put(movieEntry);
			}
			
			obj.put("movies", movies);
			
			session.close();
		}
		
		return obj;
	}
	
	// ========================================================================
	// EXISTS Methods
	// ========================================================================
	/**
	 * Checks if actorId exists in the database.
	 * @param actorId
	 * @return true if actorId exists in database, false otherwise
	 */
	public boolean actorIdExists(String actorId) {
		boolean exists = true;
		try (Session session = driver.session()){
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (n:actor{id: $x}) RETURN n",
					parameters("x", actorId)));
			
			// check if there is another node with same actor id
			if (result.list().isEmpty()) {
				// no node with same id, so actor id does not exist 
				exists = false;
			}

			session.close();
		}
		
		return exists;
	}
	
	/**
	 * Checks if movieId exists in database.
	 * @param movieId
	 * @return true if movieId exists in database, false otherwise
	 */
	public boolean movieIdExists(String movieId) {
		boolean exists = true;
		try (Session session = driver.session()){
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (n:movie{id: $x}) RETURN n",
					parameters("x", movieId)));
			
			// check if there is another node with same movie id
			if (result.list().isEmpty()) {
				// no node with same id, so movie id does not exist 
				exists = false;
			}

			session.close();
		}
		
		return exists;
	}
	
	/**
	 * Checks if the relationship ACTED_IN from actor node to movie
	 * node exists
	 * @param actorId
	 * @param movieId
	 * @return true if relationship exists, false otherwise
	 */
	public boolean relationshipExists(String actorId, String movieId) {
		boolean exists = true;
		try (Session session = driver.session()){
			StatementResult result = session.writeTransaction(tx -> tx.run(
					"MATCH (a:actor{id: $x})-[r:ACTED_IN]->(m:movie{id: $y})\n"
					+ "RETURN r;",
					parameters("x", actorId, "y", movieId)));
			
			// check if there is an existing relationship between actor and movie
			if (result.list().isEmpty()) {
				// no relationship exists
				exists = false;
			}

			session.close();
		}
		
		return exists;
	}	
	
}

/*
 * EECS3311, Project
 */
package ca.yorku.eecs;

import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;
import com.sun.net.httpserver.*;

public class Handler implements HttpHandler{
	// Neo4jMovies object will perform all the queries on the Neo4j database
	Neo4jMovies db;
	
	/**
	 * Handler Constructor
	 */
	public Handler() {
		// create Neo4jMovies object that will interact with database
		this.db = new Neo4jMovies();
	}
	
	/**
	 * Handle the given request by using the chain of responsibility
	 * design pattern to pass request to the proper GET or PUT handler
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	@Override
	public void handle(HttpExchange request) throws IOException {
		try {
			// chain of responsibility
			if (request.getRequestMethod().equals("GET")) {
				handleGet(request);
			}
			else if (request.getRequestMethod().equals("PUT")) {
				handlePut(request);
			}
			else {
				sendString(request, "Unimplemented method\n", 501);
			}
		} catch (Exception e) {
			e.printStackTrace();
			sendString(request, "Server error\n", 500);
		}
	}
	
	/**
	 * Handle the GET request by using the chain of responsibility 
	 * design pattern to pass request to the proper GET handler
	 * according to the path
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGet(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();        
        String path = uri.getPath();
        
        // pass request to correct GET handler using chain of responsibility
        if (path.equals("/api/v1/getActor")) {
        	handleGetActor(request);
        }
        else if (path.equals("/api/v1/getMovie")) {
        	handleGetMovie(request);
        }
        else if (path.equals("/api/v1/hasRelationship")) {
        	handleGetHasRelationship(request);
        }
        else if (path.equals("/api/v1/computeBaconNumber")) {
        	handleGetComputeBaconNumber(request);
        }
        else if (path.equals("/api/v1/computeBaconPath")) {
        	handleGetComputeBaconPath(request);
        }
        else if (path.equals("/api/v1/getHighestRatedMovie")) {
        	handleGetHighestRatedMovie(request);
        }
        else if (path.equals("/api/v1/getSortedMoviesForActor")) {
        	handlegetSortedMoviesForActor(request);
        }
        else {
        	sendString(request, "Unimplemented method\n", 501);
        }

	}

	/**
	 * Handle the PUT request by using the chain of responsibility 
	 * design pattern to pass request to the proper PUT handler
	 * according to the path
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handlePut(HttpExchange request) throws IOException {
        URI uri = request.getRequestURI();        
        String path = uri.getPath();
        
        // pass request to correct PUT handler using chain of responsibility
        if (path.equals("/api/v1/addActor")) {
        	handleAddActor(request);
        }
        else if (path.equals("/api/v1/addMovie")) {
        	handleAddMovie(request);
        }
        else if (path.equals("/api/v1/addRelationship")) {
        	handleAddRelationship(request);
        }
        else {
        	sendString(request, "Unimplemented method\n", 501);
        }
	}
	
	
	// ========================================================================
	// PUT Request Handlers
	// ========================================================================
	/**
	 * Handle the PUT request for the path "/api/v1/addActor".
	 * This endpoint is to add an actor node into the database
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleAddActor(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String name;
			String actorId;
			
			// validate input
			if (deserialized.has("name") && deserialized.has("actorId")) {
				name = deserialized.getString("name");
				actorId = deserialized.getString("actorId");
				
				// check if actorId already exists
				if (db.actorIdExists(actorId)) {
					// 400 BAD REQUEST, actorId already exists 
					sendString(request, "", 400);
					return;
				}
				
				// add node to database
				db.insertActor(name, actorId);
				sendString(request, "", 200);
			}
			else {
				// 400 BAD REQUEST
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
		
	}

	/**
	 * Handle the PUT request for the path "/api/v1/addMovie".
	 * This endpoint is to add a movie node into the database
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleAddMovie(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String name;
			String movieId;
			Double rating = -1.0;

			// validate input
			if (deserialized.has("name") && deserialized.has("movieId")) {
				name = deserialized.getString("name");
				movieId = deserialized.getString("movieId");
				
				// check if movieId already exists
				if (db.movieIdExists(movieId)) {
					// 400 BAD REQUEST, movieId already exists
					sendString(request, "", 400);
					return;
				}
				
				// get rating, if any
				if (deserialized.has("rating")) {
					rating = deserialized.getDouble("rating");
				}
				
				// add node to database
				db.insertMovie(name, movieId, rating);
				sendString(request, "", 200);
			}
			else {
				// 400 BAD REQUEST
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * Handle the PUT request for the path "/api/v1/addRelationship".
	 * This endpoint is to add a an ACTED_IN relationship from an actor to a 
	 * movie in the database
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleAddRelationship(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String actorId;
			String movieId;
			
			// validate input
			if (deserialized.has("actorId") && deserialized.has("movieId")) {
				actorId = deserialized.getString("actorId");
				movieId = deserialized.getString("movieId");
				
				// check if actorId and movieId exists
				if (!db.actorIdExists(actorId) || !db.movieIdExists(movieId)) {
					// 404 NOT FOUND, actor or movie does not exist
					sendString(request, "", 404);
					return;
				}
				
				// check if relationship already exists
				if (db.relationshipExists(actorId, movieId)) {
					// 400 BAD REQUEST, relationship already exists
					sendString(request, "", 400);
					return;
				}
				
				// add node to database
				db.insertRelationship(actorId, movieId);
				sendString(request, "", 200);
			}
			else {
				// 400 BAD REQUEST
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}

	// ========================================================================
	// GET Request Handlers
	// ========================================================================
	/**
	 * Handle the GET request for the path "/api/v1/getActor".
	 * This endpoint is to check if an actor exists in the database and 
	 * sends the request the actorId, name, and movieIds of the movies 
	 * the actor has acted in.
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetActor(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String actorId;
			
			// validate input
			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
				
				// check if actorId exists
				if (db.actorIdExists(actorId)) {
					// actorId exists so query database
					JSONObject obj = db.getActor(actorId);
					sendString(request, obj.toString(), 200);
					return;
				}
				
				// 404 NOT FOUND, no actor with actorId exists in database
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, actorId not provided
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * Handle the GET request for the path "/api/v1/getMovie".
	 * This endpoint is to check if a movie exists in the database and 
	 * sends the request the movieId, name, and actorIds of the actors 
	 * who act in the movie.
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetMovie(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String movieId;
			
			// validate input
			if (deserialized.has("movieId")) {
				movieId = deserialized.getString("movieId");
				
				// check if movieId exists
				if (db.movieIdExists(movieId)) {
					// movieId exists so query database
					JSONObject obj = db.getMovie(movieId);
					sendString(request, obj.toString(), 200);
					return;
				}
				
				// 404 NOT FOUND, no movie with movieId exists in database
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, movieId not provided
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}

	/**
	 * Handle the GET request for the path "/api/v1/hasRelationship".
	 * This endpoint is to check if there exists a relationship between
	 * an actor and a movie. The response will contain the actorId, movieId,
	 * and a boolean value representing the existance of a relationship between
	 * the two nodes.
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetHasRelationship(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String movieId;
			String actorId;
			
			// validate input
			if (deserialized.has("movieId") && deserialized.has("actorId")) {
				movieId = deserialized.getString("movieId");
				actorId = deserialized.getString("actorId");
				
				// check if movieId and actorId exists
				if (db.movieIdExists(movieId) && db.actorIdExists(actorId)) {
					JSONObject obj = new JSONObject();
					obj.put("movieId", movieId);
					obj.put("actorId", actorId);
					
					// check if relationship exists 
					if (db.relationshipExists(actorId, movieId)) {
						obj.put("hasRelationship", true);
					}
					else {
						obj.put("hasRelationship", false);
					}
					
					sendString(request, obj.toString(), 200);
					return;
				}
		
				// 404 NOT FOUND, movieId or actorId does not exist
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, missing required information
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}

	/**
	 * Handle the GET request for the path "/api/v1/computeBaconNumber".
	 * This endpoint is to calculate and send the bacon number of an actor.
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetComputeBaconNumber(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String actorId;
			
			// validate input
			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
				
				// check if actorId exists
				if (db.actorIdExists(actorId) || isBaconId(actorId)) {
					JSONObject obj = db.getBaconNumber(actorId);
					
					// check if there is a bacon path
					if (obj == null) {
						// 404 NOT FOUND, no path from actor to Kevin Bacon
						sendString(request, "", 404);
						return;
					}
					
					sendString(request, obj.toString(), 200);
					return;
				}
				
				// 404 NOT FOUND, no actor with actorId exists in database
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, actorId not provided
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * Handle the GET request for the path "/api/v1/computeBaconPath".
	 * This endpoint returns the shortest Bacon Path in order from the actor
	 * to Kevin Bacon. The path will interchange between actorIds and movieIds
	 * beginning with the actorId and ending with Kevin Bacon's actorId, "nm0000102"
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetComputeBaconPath(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String actorId;
			
			// validate input
			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
				
				// check if actorId exists
				if (db.actorIdExists(actorId) || isBaconId(actorId)) {
					// actorId exists so query database
					JSONObject obj = db.getBaconPath(actorId);
					
					if (obj == null) {
						// 404 NOT FOUND, no path between actor and Kevin Bacon
						sendString(request, "", 404);
						return;
					}
					
					sendString(request, obj.toString(), 200);
					return;
				}
				
				// 404 NOT FOUND, actorId does not exist in database
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, actorId not provided
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * New Feature: getHighestRatedMovie
	 * Handle the GET request for the path "/api/v1/getHighestRatedMovie".
	 * This endpoint is to retrieve the movie node(s) with the highest rating out
	 * of all the movies in the database. 
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handleGetHighestRatedMovie(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject obj = db.getHighestRatedMovie();
					
			if (obj == null) {
				// 404 NOT FOUND, no movie nodes or movie nodes with property ratings in database
				sendString(request, "", 404);
				return;
			}
			
			sendString(request, obj.toString(), 200);
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * New Feature: getSortedMoviesForActor
	 * Handle the GET request for the path "/api/v1/getSortedMoviesForActor".
	 * This endpoint is to retrieve a list of movies for an actor, sorted
	 * from the highest rating to the lowest.
	 * @param request sent from the client and used to send response
	 * @throws IOException
	 */
	private void handlegetSortedMoviesForActor(HttpExchange request) throws IOException {
		// get request body
		String body = Utils.convert(request.getRequestBody());
		
		try {
			JSONObject deserialized = new JSONObject(body);			
			String actorId;
			
			// validate input
			if (deserialized.has("actorId")) {
				actorId = deserialized.getString("actorId");
				
				// check if actorId exists
				if (db.actorIdExists(actorId) || isBaconId(actorId)) {
					JSONObject obj = db.getSortedMoviesForActor(actorId);
					
					sendString(request, obj.toString(), 200);
					return;
				}
				
				// 404 NOT FOUND, actorId does not exist in database
				sendString(request, "", 404);
			}
			else {
				// 400 BAD REQUEST, actorId not provided
				sendString(request, "", 400);
			}
			
		} catch (JSONException e) {
			// 500 INTERNAL SERVER ERROR
			e.printStackTrace();
			sendString(request, "", 500);
		}
	}
	
	/**
	 * Evaluates if the given actorId corresponds to Kevin Bacon's id
	 * @param actorId
	 * @return true if given actorId is Kevin Bacon's id, false otherwise
	 */
	private boolean isBaconId(String actorId) {
		String baconId = "nm0000102";
		
		return baconId.equals(actorId);
	}
	
	/**
	 * Taken from the REST Demo Code given on eclass
	 * @param request sent from the client and used to send response
	 * @param data a string response to send to client 
	 * @param restCode the response code to send to client
	 * @throws IOException
	 */
	private void sendString(HttpExchange request, String data, int restCode) 
			throws IOException {
		request.sendResponseHeaders(restCode, data.length());
        OutputStream os = request.getResponseBody();
        os.write(data.getBytes());
        os.close();
	}

}

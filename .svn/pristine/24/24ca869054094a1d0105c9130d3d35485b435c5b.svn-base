package se.su.dsv.maryam;

import java.util.List;

public interface DatabaseMethods {
	/**
	 * A method to get all the available start points 
	 * @return A list of available start points 
	 */
	public List<Point> getStartPoints();
	
	/**
	 * Gets you the title of the current route
	 * @return The current start point
	 */
	public String getCurrentRouteTitle();
	
	/**
	 * @param routeTitle The title of the current route
	 */
	public void setCurrentRouteTitle(String routeTitle);
	
	/**
	 * Gets all the points of a route, including the start
	 * point which will be at index 0.
	 * @param routeTitle The title of the requested route
	 * @return A list of points containing the route
	 */
	public List<Point> getPoints(String routeTitle);
	
	/**
	 * Returns a list of all the points being visited in 
	 * the current route the user is traveling 
	 * @return A list of visited routes
	 */
	public List<Point> getVisitedPoints();
	
	/**
	 * Adds a point the the list of visited points
	 * @param point Point to be added to the list
	 */
	public void addPointToVisited(Point point);
	
	/**
	 * @return The filename of the file that should be looping 
	 * in the background
	 */
	public String getNextBackgroundSound();
	
	/**
	 * Used to set the name of the file that should start
	 * plying after the current scene. Also helpful for 
	 * bringing the application back to its current state
	 * after a crash
	 * @param fileName Name of the file to be played in the
	 * background
	 */
	public void setNextBackgroundSound(String fileName);
}

package ContentAddressableNetwork;
import java.io.Serializable;

/**
 * File: Point.java
 * 
 * Maintains Points of a 2D coordinate space
 * @author Binit
 *
 */
@SuppressWarnings("serial")
public class Point implements Serializable{

	private double x;
	private double y;
	
	public Point(double x, double y){
		setX(x);
		setY(y);
	}
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
}

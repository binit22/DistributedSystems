package hw2;
import java.util.concurrent.Semaphore;

/**
 * @Filename Hotdog.java
 *
 * @Version $Id: Hotdog.java,v 1.0 2014/04/13 09:23:00 $
 *
 * @Revisions
 *     Initial Revision
 */

/**
 * This program simulates a hotdog maker using semaphores.There are 3 hotdog makers and one supplier.
 * Supplier puts 2 items at a time on the table. Each maker has one item and are waiting for other 
 * two items to make a hotdog. We have to simulate this process in such a way that only one maker can 
 * take items from the table at a time.
 * 
 * @author Binit Shah
 */
public class Hotdog extends Thread{

	public String name;
	// initially all three ingredients are on the table
	public static Integer[] item = {1,1,1};

	public static Semaphore table = new Semaphore(1);
	public boolean maker1;
	public boolean maker2;
	public boolean maker3;

	/**
	 * @param name	name of the thread
	 */
	public Hotdog(String name){
		this.name = name;
	}

	/**
	 * simulates the hotdog makers such that only one maker at a time can take items from the table
	 */
	public void hotdogMaker(){

		try{
			if(this.name.equals("Sausage")){
				while(true){
					if(!maker1)
						System.out.println("Maker 1 has Sausage...");
					maker1 = true;

					// try to acquire lock on table so that only this maker can take items from table
					if(table.tryAcquire()){
						synchronized(item){
							// wait until Bun is placed on the table by supplier
							while(item[1] == 0){}
							System.out.println("Maker 1 got Bun...");
							// wait until Mustard is placed on the table by supplier
							while(item[2] == 0){}
							System.out.println("Maker 1 got Mustard...");
							// reset item count
							item[1] = 0;	item[2] = 0;	maker1 = false;
							System.out.println("Maker 1 makes Hotdog!!");
							// release lock
							table.release();
							Thread.sleep(1000);
						}
					}
				}
			}



			if(this.name.equals("Bun")){
				while(true){
					if(!maker2)
						System.out.println("Maker 2 has Bun...");
					maker2 = true;

					// try to acquire lock on table so that only this maker can take items from table
					if(table.tryAcquire()){
						synchronized(item){
							// wait until Sausage is placed on the table by supplier
							while(item[0] == 0){}
							System.out.println("Maker 2 got Sausage...");
							// wait until Mustard is placed on the table by supplier
							while(item[2] == 0){}
							System.out.println("Maker 2 got Mustard...");
							// reset item count
							item[0] = 0;	item[2] = 0;	maker2 = false;
							System.out.println("Maker 2 makes Hotdog!!");
							// release lock
							table.release();
							Thread.sleep(1000);
						}
					}
				}
			}

			if(this.name.equals("Mustard")){
				while(true){
					if(!maker3)
						System.out.println("Maker 3 has Mustard...");
					maker3 = true;

					// try to acquire lock on table so that only this maker can take items from table
					if(table.tryAcquire()){
						synchronized(item){
							// wait until Sausage is placed on the table by supplier
							while(item[0] == 0){}
							System.out.println("Maker 3 got Sausage...");
							// wait until Bun is placed on the table by supplier
							while(item[1] == 0){}
							System.out.println("Maker 3 got Bun...");
							// reset item count
							item[0] = 0;	item[1] = 0;	maker3 = false;
							System.out.println("Maker 3 makes Hotdog!!");
							// release lock
							table.release();
							Thread.sleep(1000);
						}
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}

	/**
	 * supplier keeps supplying two ingredients at a time on the table 
	 */
	public void hotdogSupplier(){
		while(true){
			try{
				int supply;
				int count = 0;
				// keep two distinct items on the table
				while(count < 2){
					supply = (int)(Math.random() * 3);
					if(item[supply] == 0){
						item[supply] = 1;
						count++;
					}
				}
				// sleep for some time
				Thread.sleep(1000);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	/**
	 * calls method for supplier when a supplier comes or calls the method for hotdog maker otherwise
	 */
	public void run(){
		if(this.name.equals("Supplier"))
			this.hotdogSupplier();
		else
			this.hotdogMaker();
	}

	/**
	 * @param args	command line arguments(Ignored).
	 */
	public static void main(String[] args) {

		// hotdog maker
		Thread maker1 = new Hotdog("Sausage");
		Thread maker2 = new Hotdog("Bun");
		Thread maker3 = new Hotdog("Mustard");
		// ingredients supplier
		Thread supplier = new Hotdog("Supplier");

		maker1.start();
		maker2.start();
		maker3.start();
		supplier.start();
	}	
}

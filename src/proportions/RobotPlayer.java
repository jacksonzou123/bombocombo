package proportions;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    static MapLocation home;
    static int turnCount;
    static MapLocation neutralCenter;
    static int neutralID;
    static int enemyID;
    static MapLocation enemyLoc;
    //HELLO

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        enemyID = -1;
        enemyLoc = null;
        neutralCenter = null;
        neutralID = -1;
        turnCount = 0;
        mode = 0;

        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to freeze
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You may rewrite this into your own control structure if you wish.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
    	Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        if (rc.canBid(1)) {
          rc.bid(1);
        }
        if (turnCount <= 50) {
          for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
              rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
              break;
            }
          }
        }

        // If muckraker has a flag thats a location
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, ally)){
        	if (robot.getType() == RobotType.MUCKRAKER){
        		int roboID = robot.ID;
        		if (rc.canGetFlag(roboID)){
        			if (checkIfFlagIsLocation(rc.getFlag(roboID))){
						if (rc.canSetFlag(rc.getFlag(roboID))) rc.setFlag(rc.getFlag(roboID));
	        		}
        		}
        	}
        }
        if (checkIfFlagIsLocation(rc)){
	        if (rc.canBuildRobot(RobotType.POLITICIAN, Direction.NORTH, 10)){
		        rc.buildRobot(RobotType.POLITICIAN, Direction.NORTH, 10);
		        System.out.println("I built POLITICIAN!");
		    }
		}
    }

    


    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            rc.empower(actionRadius);
            return;
        }
        if (tryMove(randomDirection())){
            //System.out.println("I moved!");
        }
        if (neutralCenter == null) {
            if (rc.canGetFlag(homeID)) {
                int ecFlag = rc.getFlag(homeID);
                if (ecFlag != 0) {
                    if (rc.canSetFlag(ecFlag)) rc.setFlag(ecFlag);
                    neutralCenter = getLocationFromFlag(ecFlag);
                }
            }
        }
        if (neutralCenter != null) {
            Direction dirToEnemy = rc.getLocation().directionTo(neutralCenter);
            if (!tryMove(dirToEnemy)) {
                tryMove(randomDirection());
            }
        }
    }

    static void runSlanderer() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        //check all nearby enemy robots
        for (RobotInfo robot: rc.senseNearbyRobots(-1, enemy)) {
          //if enemy robot is a muckraker, run the opposite direction
          if (robot.getType() == RobotType.MUCKRAKER) {
            Direction enemy_direction = rc.getLocation().directionTo(robot.location);
            Direction move = enemy_direction.opposite();
            if (smartMove(move)) {
             	return; 
            }
          }
        }
        if (tryMove(randomDirection())) {
            //System.out.println("I moved!");
        }
    }

    static void runMuckraker() throws GameActionException {
        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                //It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    //System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }

        //scouting mode
        //enemyLoc should be NULL in this mode
        if (mode == 0) {
            //set home location on spawn
            if (homeLoc == null) {
              for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                  homeID = robot.ID;
                  homeLoc = robot.location;
                }
              }
            }
            //runs if home does not have target location
            if (rc.canGetFlag(homeID) && rc.getFlag(homeID) == 0) {
                for (RobotInfo robot: rc.senseNearbyRobots(-1)) {
                    if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    	if (robot.getTeam() == enemy){
                    		enemyLoc = robot.location;
	                        enemyID = robot.ID;
	                        rc.setFlag(pushLocationToFlag(robot.location));
	                        mode = 1;
                    	}
                        else if (robot.getTeam() == Team.NEUTRAL){
			          		neutralCenter = robot.location;
			          		neutralID = robot.ID;
			          		rc.setFlag(pushLocationToFlag(robot.location));
			          		System.out.println("There is a neutral center near here!");
			          		mode = 1;
		          		}
                    }
                    
                }
                if (tryMove(randomDirection())){
                      //System.out.println("I moved!");
                }
            }
            //runs if home already has target location
            else {
                mode = 2;
                if (tryMove(randomDirection())){
                      //System.out.println("I moved!");
                }
            }
        }

        //return mode
        //flag is set to location of enemy ec
        if (mode == 1) {
            //runs if home does not have target location
            if (rc.canGetFlag(homeID) && rc.getFlag(homeID) == 0) {
                Direction toHome = rc.getLocation().directionTo(homeLoc);
                if (tryMove(toHome)) {
                    return;
                }
                if (tryMove(toHome.rotateLeft())) {
                    return;
                }
                if (tryMove(toHome.rotateRight())) {
                    return;
                }
            }
            //runs if home does have target location
            else {
                mode = 2;
            }
        }

        //random mode
        //already did its job, walking around randomly
        if (mode == 2) {
            if (tryMove(randomDirection())){
                  //System.out.println("I moved!");
            }
        }
    }

    /**
     * Returns a random Direction.
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }

    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean smartMove(Direction dir) throws GameActionException{
  	if (tryMove(dir)) {
          System.out.println("imma skeddadle");
          //System.out.println("I moved");
          return true;
        }
        else if(tryMove(dir.rotateLeft())){
          System.out.println("I skeddaddle the other way");
          return true;
        }
        else if(tryMove(dir.rotateRight())){
          System.out.println("I skeddaddle the other other way");
          return true;
        }
        return false;
  }

  static MapLocation getLocationFromFlag(int flag) {
        MapLocation currentLocation = rc.getLocation();
        int offsetX = currentLocation.x / 128;
        int offsetY = currentLocation.y / 128;

        int targetX = (flag / 128) % 128;
        int targetY = flag % 128;
        MapLocation actualLocation = new MapLocation(offsetX * 128 + targetX, offsetY * 128 + targetY);

        MapLocation alternative = actualLocation.translate(-128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(128, 0);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, -128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }
        alternative = actualLocation.translate(0, 128);
        if (rc.getLocation().distanceSquaredTo(alternative) < rc.getLocation().distanceSquaredTo(actualLocation)) {
            actualLocation = alternative;
        }

        return alternative;
    }

    static int pushLocationToFlag(MapLocation location) {
        return (location.x % 128) * 128 + (location.y % 128);
    }
}

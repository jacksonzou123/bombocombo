package v3;
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
    //store ID and location of an ally and home enlightenment center
    static int homeID;
    static int enemyID;
    static MapLocation homeLoc;
    static MapLocation enemyLoc;

    //store ID of ally muckraker that you're reading from
    static int unitID;

    //mode of unit (determintes what they're doing)
    static int mode;

    static int turnCount;

    static Direction cameFrom;
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

        homeID = -1;
        enemyID = -1;
        unitID = -1;
        homeLoc = null;
        enemyLoc = null;
        turnCount = 0;
        mode = 0;
        cameFrom = null;

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
                //System.out.println("HOME:" + home.x + ", " + home.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        //set home equal to its own location
        if (homeID == -1) homeID = rc.getID();
        if (homeLoc == null) homeLoc = rc.getLocation();

        //bid
        if (rc.getTeamVotes() < 751) {
            if (rc.canBid((int)Math.ceil((rc.getInfluence() * 0.005)))) rc.bid((int)Math.ceil((rc.getInfluence() * 0.005)));
        }

        //build muckrakers for first 100 turns
        if (turnCount <= 100) {
          for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
              rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
              break;
            }
          }
        }
        //build other stuff
        else {
          RobotType toBuild = randomSpawnableRobotType();
          for (Direction dir : directions) {
              if (rc.canBuildRobot(toBuild, dir, 150) && toBuild == RobotType.POLITICIAN) {
                  rc.buildRobot(toBuild, dir, 150);
                  break;
              } else if (rc.canBuildRobot(toBuild, dir, 41) && toBuild == RobotType.SLANDERER) {
                  rc.buildRobot(toBuild, dir, 41);
                  break;
              } else if (rc.canBuildRobot(toBuild, dir, 1) && toBuild == RobotType.MUCKRAKER) {
                  rc.buildRobot(toBuild, dir, 1);
                  break;
              }
                else {
                  break;
              }
           }
        }

        // looks for ally muckrakers with flag that contain info about enemy EC
        // sets flag if messenger muckrakers have info about enemy EC
        if (mode == 0) {
            if (unitID == -1) {
                for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                    if (robot.type == RobotType.MUCKRAKER){
                        int otherflag = rc.getFlag(robot.getID());
                        if (otherflag != 0) {
                            unitID = robot.getID();
                            int tempID = getIDFromFlag(otherflag);
                            if (tempID < 0) {
                                enemyID = tempID * -512;
                            }
                            else {
                                enemyID = tempID;
                            }
                            enemyLoc = getLocationFromFlag(otherflag);
                            //System.out.println("1: " + tempID);
                            return;
                        }
                    }
                }
            }
            else {
                if (rc.canGetFlag(unitID)) {
                    int tempID = getIDFromFlag(rc.getFlag(unitID));
                    if (tempID < 0) {
                        enemyID += tempID * -512;
                    }
                    else {
                        enemyID += tempID;
                    }
                    //System.out.println("2: " + tempID);
                    mode = 1;
                    //System.out.println("EnemyID: " + enemyID);
                }
                else {
                    enemyID = -1;
                    enemyLoc = null;
                    unitID = -1;
                }
            }

        }


        if (mode == 1) {
            if (rc.canGetFlag(enemyID)) {
                int flagnum = pushLocationToFlag(enemyLoc);
                if (rc.canSetFlag(flagnum)) rc.setFlag(flagnum);
            }
            else {
                enemyID = -1;
                enemyLoc = null;
                unitID = -1;
                if (rc.canSetFlag(0)) rc.setFlag(0);
                mode = 0;
            }
            return;
        }
    }

    static void runPolitician() throws GameActionException {
        // set home equal to the EC that built it
        // if home EC has enemy EC loc, set flag and enemyLoc
        if (homeLoc == null) {
            for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    homeID = robot.ID;
                    homeLoc = robot.location;
                    break;
                }
            }
        }

        if (enemyLoc == null) {
            if (rc.canGetFlag(homeID)) {
                int ecFlag = rc.getFlag(homeID);
                if (ecFlag != 0) {
                    if (rc.canSetFlag(ecFlag)) rc.setFlag(ecFlag);
                    enemyLoc = getLocationFromFlag(ecFlag);
                }
            }
        }

        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] robotsNearby = rc.senseNearbyRobots(actionRadius);
        int enemyCount = 0;
        for (RobotInfo robot: robotsNearby) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                if (robot.getTeam() == enemy || robot.getTeam() == Team.NEUTRAL){
                    if(rc.canEmpower(actionRadius)) {
                        //System.out.println("jeff say go attek");
                        rc.empower(actionRadius);
                        return;
                    }
                }
            }
            if (robot.team == enemy) {
                enemyCount++;
            }
        }

        // if it has an enemyLoc, move towards enemyLoc
        if (enemyLoc != null) {
            Direction dirToEnemy = rc.getLocation().directionTo(enemyLoc);
            if (smartMove(dirToEnemy)) {
                return;
            }
        }

        if (enemyCount > 6) {
            if (rc.canEmpower(actionRadius)) {
                rc.empower(actionRadius);
                return;
            }
        }
        else {
            //System.out.println("skrting closer");
            int[] enemyCounter = new int[8];
            MapLocation location = rc.getLocation();
            for (RobotInfo robot: robotsNearby) {
                for (int i = 0; i < 8; i++) {
                    MapLocation newLocation = location.add(directions[i]);
                    if (newLocation.distanceSquaredTo(robot.location) <= actionRadius) {
                        enemyCounter[i]++;
                    }
                }
            }
            int maxEnemySpot = 0;
            for (int i = 0; i < 8; i++) {
                if (enemyCounter[i] > enemyCounter[maxEnemySpot]) {
                    maxEnemySpot = i;
                }
            }
            smartMove(directions[maxEnemySpot]);
            return;
        }
    }

    static void runSlanderer() throws GameActionException {
        if (homeLoc == null) {
          for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
              homeID = robot.ID;
              homeLoc = robot.location;
            }
          }
        }

        Team enemy = rc.getTeam().opponent();
        //check all nearby enemy robots
        for (RobotInfo robot: rc.senseNearbyRobots(-1, enemy)) {
          //if enemy robot is a muckraker, run the opposite direction
          if (robot.getType() == RobotType.MUCKRAKER) {
            Direction enemy_direction = rc.getLocation().directionTo(robot.location);
            final Direction move = enemy_direction.opposite();
            if (tryMove(move)) {
              //System.out.println("imma skeddadle");
              //System.out.println("I moved");
              return;
            }
            else if(tryMove(move.rotateLeft())){
              //System.out.println("I skeddaddle the other way");
              return;
            }
            else if(tryMove(move.rotateRight())){
              //System.out.println("I skeddaddle the other other way");
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
                        if (robot.getTeam() == enemy || robot.getTeam() == Team.NEUTRAL){
                            enemyLoc = robot.location;
                            enemyID = robot.ID;
                            rc.setFlag(pushIDToFlag(turnCount % 2, enemyID) + pushLocationToFlag(robot.location));
                            mode = 1;
                        }
                    }
                }
                muckMove();
            }
            //runs if home already has target location
            else {
                mode = 2;
                muckMove();
            }
        }

        //return mode
        //flag is set to location of enemy ec
        if (mode == 1) {
            //runs if home does not have target location
            if (rc.canGetFlag(homeID) && rc.getFlag(homeID) == 0) {
                rc.setFlag(pushIDToFlag(turnCount % 2, enemyID) + pushLocationToFlag(enemyLoc));
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
            if (rc.canGetFlag(homeID) && rc.getFlag(homeID) == 0) {
                mode = 0;
                enemyID = -1;
                enemyLoc = null;
                if (rc.canSetFlag(0)) rc.setFlag(0);
            }
            else muckMove();
        }
    }

    static void muckMove() throws GameActionException{
      boolean moved = false;
      while(!moved){
        if(cameFrom == null){
          Direction tempdir = randomDirection();
          if(tryMove(tempdir)){
            cameFrom = tempdir.opposite();
            moved = true;
          }
        }
        else{
          Direction tempdir = randomDirection();
          if(tempdir != cameFrom && tempdir != cameFrom.rotateLeft() && tempdir != cameFrom.rotateRight() && tryMove(tempdir)){
            cameFrom = tempdir.opposite();
            moved = true;
          }
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

    static int flagnumFromDir(Direction dir) throws GameActionException {
        return java.util.Arrays.asList(directions).indexOf(dir);
    }

    //returns part of the ID from the flag
    //if value is positive, then it is the id mod 512
    //if value is negative, then it is the id divided 512
    static int getIDFromFlag(int flag) {
        flag = flag / 16384; //2^14
        if (flag > 512) {
            return (flag % 512) * -1;
        }
        return flag;
    }

    static MapLocation getLocationFromFlag(int flag) {
        flag = flag % 16384; //2^14
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

        return actualLocation;
    }

    //returns the int to put into flag based on ID given
    //turn is 0 or 1, based on odd or even turn
    static int pushIDToFlag(int turn, int id) {
        if (turn == 0) {
            return (id % 512) << 14;
        }
        return ((1 << 9) | (id / 512)) << 14;
    }

    //returns the int to put into flag based on location given
    static int pushLocationToFlag(MapLocation location) {
        return (location.x % 128) * 128 + (location.y % 128);
    }

    static boolean smartMove(Direction dir) throws GameActionException{
      if (tryMove(dir)) {
            //System.out.println("imma skeddadle");
            //System.out.println("I moved");
            return true;
          }
          else if(tryMove(dir.rotateLeft())){
            //System.out.println("I skeddaddle the other way");
            return true;
          }
          else if(tryMove(dir.rotateRight())){
            //System.out.println("I skeddaddle the other other way");
            return true;
          }
          return false;
    }
}

package jacksonisgonnalosehisshitdoingthisstupidassproject;
import battlecode.common.*;
import java.util.Random;

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

    static MapLocation homeLoc;
    static int homeID;

    static RobotType lastRobotMade;

    static Direction lastMove;
    static Direction target;

    static boolean alert;

    static int turnCount;
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
        homeLoc = null;
        homeID = -1;
        lastRobotMade = null;
        alert = false;
        target = null;
        lastMove = null;

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
        if (homeID == -1) {
            homeID = rc.getID();
            homeLoc = rc.getLocation();
        }

        //count ally bots
        RobotInfo[] allys = rc.senseNearbyRobots(-1, rc.getTeam());
        int[] allyBots = new int[4];
        for (RobotInfo robot : allys) {
            switch (robot.type) {
                case POLITICIAN: allyBots[0]++; break;
                case SLANDERER: allyBots[1]++; break;
                case MUCKRAKER: allyBots[2]++; break;
                case ENLIGHTENMENT_CENTER: allyBots[3]++; break;
            }
        }

        //count enemy bots
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int[] enemyBots = new int[4];
        for (RobotInfo robot : enemies) {
            switch (robot.type) {
                case POLITICIAN: enemyBots[0]++; break;
                case SLANDERER: enemyBots[1]++; break;
                case MUCKRAKER: enemyBots[2]++; break;
                case ENLIGHTENMENT_CENTER: enemyBots[3]++; break;
            }
        }

        if (lastRobotMade == null) {
            for (Direction dir: directions) {
                if (rc.canBuildRobot(RobotType.SLANDERER, dir, calculateSlanderInfluence(rc.getInfluence()))) {
                    rc.buildRobot(RobotType.SLANDERER, dir, calculateSlanderInfluence(rc.getInfluence()));
                    lastRobotMade = RobotType.SLANDERER;
                    return;
                }
            }
        }

        if (lastRobotMade == RobotType.SLANDERER) {
            for (Direction dir: directions) {
                if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
                    rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
                    lastRobotMade = RobotType.MUCKRAKER;
                    return;
                }
            }
        }

        if (lastRobotMade == RobotType.MUCKRAKER) {
            for (Direction dir: directions) {
                int influence = (int)(rc.getInfluence() * 0.1);
                if (rc.canBuildRobot(RobotType.POLITICIAN, dir, influence)) {
                    rc.buildRobot(RobotType.POLITICIAN, dir, influence);
                    lastRobotMade = RobotType.POLITICIAN;
                    return;
                }
            }
        }

        if (lastRobotMade == RobotType.POLITICIAN) {
            for (Direction dir: directions) {
                if (rc.canBuildRobot(RobotType.SLANDERER, dir, calculateSlanderInfluence(rc.getInfluence()))) {
                    rc.buildRobot(RobotType.SLANDERER, dir, calculateSlanderInfluence(rc.getInfluence()));
                    lastRobotMade = RobotType.SLANDERER;
                    return;
                }
            }
        }


    }

    static void runPolitician() throws GameActionException {
        RobotInfo[] allyBots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemyBots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        // set home equal to the EC that built it
        if (homeLoc == null) {
            for (RobotInfo robot : allyBots) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    homeID = robot.ID;
                    homeLoc = robot.location;
                    Direction toHome = rc.getLocation().directionTo(homeLoc);
                    target = toHome.opposite();
                    break;
                }
            }
        }

        if (target == null) {
            Random rand = new Random();
            target = directions[rand.nextInt(8)];
        }

        if (!rc.onTheMap(rc.getLocation().add(target))) {
            target = target.rotateRight().rotateRight();
        }
        smartMove(target);
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
        if (rc.getLocation().distanceSquaredTo(homeLoc) < 25) {
            if (tryMove(randomDirection())) {
                //System.out.println("I moved!");
            }
        }
    }

    static void runMuckraker() throws GameActionException {
        RobotInfo[] allyBots = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemyBots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        if (homeLoc == null) {
            for (RobotInfo robot: allyBots) {
                if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
                    homeID = robot.ID;
                    homeLoc = robot.location;
                    Direction toHome = rc.getLocation().directionTo(robot.location);
                    target = toHome.opposite();
                    break;
                }
            }
        }

        //search for slanderers to expose and look if any nearby enemy politicians
        for (RobotInfo robot: enemyBots) {
            if (robot.type == RobotType.SLANDERER) {
                if (rc.canExpose(robot.ID)) {
                    rc.expose(robot.ID);
                }
            }
            if (robot.type == RobotType.POLITICIAN) {
                alert = true;
            }
        }

        //look to protect home EC if there are nearby enemy politicians and muckraker is near home
        if (alert == true && rc.canSenseLocation(homeLoc)) {
            int countCloserMuckrakers = 0;
            for (RobotInfo robot : allyBots) {
                if (robot.type == RobotType.MUCKRAKER && (robot.location.distanceSquaredTo(homeLoc) < rc.getLocation().distanceSquaredTo(homeLoc))) {
                    countCloserMuckrakers += 1;
                }
            }
            if (countCloserMuckrakers > 4) {
                alert = false;
            }
        }

        if (alert == true) {
            if (rc.getLocation().distanceSquaredTo(homeLoc) > 1) {
                smartMove(rc.getLocation().directionTo(homeLoc));
            }
            int flag = defense(alert);
            if (rc.canSetFlag(flag)) {
                rc.setFlag(flag);
            }
        }
        else {
            if (!rc.onTheMap(rc.getLocation().add(target))) {
                target = target.rotateRight().rotateRight();
            }
            smartMove(target);
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
            lastMove = dir.opposite();
            return true;
          }
          else if(tryMove(dir.rotateLeft())){
            //System.out.println("I skeddaddle the other way");
            lastMove = dir.rotateLeft().opposite();
            return true;
          }
          else if(tryMove(dir.rotateRight())){
            //System.out.println("I skeddaddle the other other way");
            lastMove = dir.rotateLeft().opposite();
            return true;
          }
          return false;
    }

    static int calculateSlanderInfluence(int influence) {
        int gain = (int)Math.floor(influence * (1.0/50 + 0.03 * Math.pow(Math.E, -0.001 * influence)));
        while ((int)Math.floor(influence * (1.0/50 + 0.03 * Math.pow(Math.E, -0.001 * influence))) == gain) {
            influence -= 1;
        }
        return influence + 1;
    }

    static int defense(boolean alert) {
        if (alert) {
            return 1 << 23;
        }
        return 0;
    }
}

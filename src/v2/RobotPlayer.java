package v2;
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

    static int homeID;
    static int enemyID;
    static MapLocation homeLoc;
    static MapLocation enemyLoc;
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

        homeID = -1;
        enemyID = -1;
        homeLoc = null;
        enemyLoc = null;
        turnCount = 0;

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
        if (homeID == -1) {
          homeID = rc.getID();
        }
        if (homeLoc == null) {
          homeLoc = rc.getLocation();
        }

        if (rc.canBid(1)) {
          rc.bid(1);
        }

        if (turnCount <= 100) {
          for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
              rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
              break;
            }
          }
        }
        else {
          RobotType toBuild = randomSpawnableRobotType();
          for (Direction dir : directions) {
              if (rc.canBuildRobot(toBuild, dir, 50) && toBuild == RobotType.POLITICIAN) {
                  rc.buildRobot(toBuild, dir, 50);
                  break;
              } else if (rc.canBuildRobot(toBuild, dir, 1)) {
                  rc.buildRobot(toBuild, dir, 1);
                  break;
              } else {
                  break;
              }
           }
        }
    }

    static void runPolitician() throws GameActionException {
        //set home equal to the enlightenment center that built it
        if (homeLoc == null) {
          for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
              homeID = robot.ID;
              homeLoc = robot.location;
            }
          }
        }

        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            System.out.println("empowering...");
            rc.empower(actionRadius);
            System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection())){
            //System.out.println("I moved!");
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
              System.out.println("imma skeddadle");
              //System.out.println("I moved");
              return;
            }
          }
        }
        if (tryMove(randomDirection())) {
            //System.out.println("I moved!");
        }
    }

    static void runMuckraker() throws GameActionException {
        if (homeLoc == null) {
          for (RobotInfo robot : rc.senseNearbyRobots(-1, rc.getTeam())) {
            if (robot.type == RobotType.ENLIGHTENMENT_CENTER) {
              homeID = robot.ID;
              homeLoc = robot.location;
            }
          }
        }

        Team ally = rc.getTeam();
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                //It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        //Check all nearby robots
        for (RobotInfo robot : rc.senseNearbyRobots(-1)) {
            //check if nearby robot is an allied muckraker with flag != 0
            if (robot.getTeam() == ally && robot.getType() == RobotType.MUCKRAKER && rc.getFlag(robot.getID()) != 0) {
                break;
            }
            //check if nearby robot is an enemy enlightenment center
            else if (robot.getTeam() == enemy && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    MapLocation enemyCenter = robot.getLocation();
                    int flagnum = flagnumFromDir(rc.getLocation().directionTo(enemyCenter));
                    if (rc.canSetFlag(flagnum)) rc.setFlag(flagnum);
                }
            }
        }
        //if your flag is 0, you can move, otherwise don't
        if (rc.getFlag(rc.getID()) == 0 && tryMove(randomDirection())){
              //System.out.println("I moved!");
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

    // Pass in dir, sets flag to number based on index of dir in directions[]
    static int flagnumFromDir(Direction dir) throws GameActionException {
        return java.util.Arrays.asList(directions).indexOf(dir) + 1;
    }

    static MapLocation getLocationFromFlag(int flag);

    static int pushLocationToFlag(MapLocation location);
}

package v6;
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

    static int turnCount;
    static MapLocation homeLoc;
    static Direction dir;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;

        turnCount = 0;
        homeLoc = null;
        dir = randomDirection();

        while (true) {
            turnCount += 1;
            try {
                switch (rc.getType()) {
                    case ENLIGHTENMENT_CENTER: runEnlightenmentCenter(); break;
                    case POLITICIAN:           runPolitician();          break;
                    case SLANDERER:            runSlanderer();           break;
                    case MUCKRAKER:            runMuckraker();           break;
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }



    static void runEnlightenmentCenter() throws GameActionException {
        if (rc.getFlag(rc.getID()) == 0) {
            int flagnum = pushLocationToFlag(rc.getLocation());
            if (rc.canSetFlag(flagnum)) rc.setFlag(flagnum);
            MapLocation location = getLocationFromFlag(flagnum);
            System.out.println("HOME EC @ ("+location.x+", "+location.y+")");
        }

        // FIRST TURN: Creates slandy with biggest possible influence to make money
        if (turnCount == 1) {
            int maxInf = calculateSlanderInfluence(rc.getInfluence());
            Direction randDir = randomDirection();
            System.out.println("BIG SLANDY: "+maxInf+" influence");
            if (rc.canBuildRobot(RobotType.SLANDERER, randDir, maxInf)) {
                rc.buildRobot(RobotType.SLANDERER, randDir, maxInf);
            }
        }
        // RobotType toBuild = randomSpawnableRobotType();
        // int influence = 50;
        // for (Direction dir : directions) {
        //     if (rc.canBuildRobot(toBuild, dir, influence)) {
        //         rc.buildRobot(toBuild, dir, influence);
        //     } else {
        //         break;
        //     }
        // }
        // if (rc.canBuildRobot(RobotType.MUCKRAKER, Direction.NORTHWEST, 1)){
        // rc.buildRobot(RobotType.MUCKRAKER, Direction.NORTHWEST, 1);
    //   }
    }

    static void runPolitician() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        RobotInfo[] attackable = rc.senseNearbyRobots(actionRadius, enemy);
        if (attackable.length != 0 && rc.canEmpower(actionRadius)) {
            //System.out.println("empowering...");
            rc.empower(actionRadius);
            //System.out.println("empowered");
            return;
        }
        if (tryMove(randomDirection())) {
            //System.out.println("I moved!");
        }
    }

    static void runSlanderer() throws GameActionException {
        int sensorRadius = rc.getType().sensorRadiusSquared;
        // Sets homeLoc variable to location of spawn EC
        if (homeLoc == null) {
            for (RobotInfo robot : rc.senseNearbyRobots(sensorRadius, rc.getTeam())) {
                if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
                    if (rc.canGetFlag(robot.getID())) {
                        homeLoc = getLocationFromFlag(rc.getFlag(robot.getID()));
                    }
                    break;
                }
            }
        }
        // Moves a little bit away from spawn EC
        if (rc.getLocation().distanceSquaredTo(homeLoc) < 4) {
            tryMove(randomDirection());
        }
        
    }

    static void runMuckraker() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int actionRadius = rc.getType().actionRadiusSquared;
        for (RobotInfo robot : rc.senseNearbyRobots(actionRadius, enemy)) {
            if (robot.type.canBeExposed()) {
                // It's a slanderer... go get them!
                if (rc.canExpose(robot.location)) {
                    //System.out.println("e x p o s e d");
                    rc.expose(robot.location);
                    return;
                }
            }
        }
        if (tryMove(randomDirection())) {
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



    static int pushLocationToFlag(MapLocation location) {
        return (location.x % 128) * 128 + (location.y % 128);
    }

    static MapLocation getLocationFromFlag(int flag) throws GameActionException {
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

    static int calculateSlanderInfluence(int influence) throws GameActionException {
        int gain = (int)Math.floor(influence * (1.0/50 + 0.03 * Math.pow(Math.E, -0.001 * influence)));
        while ((int)Math.floor(influence * (1.0/50 + 0.03 * Math.pow(Math.E, -0.001 * influence))) == gain) {
            influence -= 1;
        }
        return influence + 1;
    }

    static int indexFromDir(Direction dir) throws GameActionException {
        return java.util.Arrays.asList(directions).indexOf(dir);
    }

    static Direction oppositeDir(Direction dir) throws GameActionException {
        int opposite = (indexFromDir(dir) + 5) % 8;
        return directions[opposite];
    }

    
}

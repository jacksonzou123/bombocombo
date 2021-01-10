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

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runEnlightenmentCenter() throws GameActionException {
        if (rc.canBid(1)) {
          rc.bid(1);
        }
        if (turnCount <= 60) {
          for (Direction dir : directions) {
            if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
              rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
              break;
            }
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
    }

    static void runSlanderer() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        //check all nearby enemy robots
        for (RobotInfo robot: rc.senseNearbyRobots(-1, enemy)) {
          //if enemy robot is a muckraker, run the opposite direction
          if (robot.getType() == RobotType.MUCKRAKER) {
            Direction enemy_direction = rc.getLocation().directionTo(robot.location);
            Direction move = enemy_direction.opposite();
            if (tryMove(move)) {
              System.out.println("imma skeddadle");
              //System.out.println("I moved");
              return;
            }
            else if(tryMove(move.rotateLeft())){
              System.out.println("I skeddaddle the other way");
              return;
            }
            else if(tryMove(move.rotateRight())){
              System.out.println("I skeddaddle the other other way");
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
        // Finding enlightenment
        for (RobotInfo robot: rc.senseNearbyRobots(actionRadius, ally)){
        	if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER){
        		//System.out.println(home.x + " " + home.y);
        	}
        }
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
            MapLocation ec = robot.getLocation();
            switch (rc.getLocation().directionTo(ec)) {
              case NORTH: if (rc.canSetFlag(1)) rc.setFlag(1); break;
              case NORTHEAST: if (rc.canSetFlag(2)) rc.setFlag(2); break;
              case EAST: if (rc.canSetFlag(3)) rc.setFlag(3); break;
              case SOUTHEAST: if (rc.canSetFlag(4)) rc.setFlag(4); break;
              case SOUTH: if (rc.canSetFlag(5)) rc.setFlag(5); break;
              case SOUTHWEST: if (rc.canSetFlag(6)) rc.setFlag(6); break;
              case WEST: if (rc.canSetFlag(7)) rc.setFlag(7); break;
              case NORTHWEST: if (rc.canSetFlag(8)) rc.setFlag(8); break;
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
}

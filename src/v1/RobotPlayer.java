package v1;
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
    if (turnCount <= 100) {
      for (Direction dir : directions) {
        if (rc.canBuildRobot(RobotType.MUCKRAKER, dir, 1)) {
          rc.buildRobot(RobotType.MUCKRAKER, dir, 1);
          break;
        }
      }
    }
    else {
      RobotType toBuild = RobotType.POLITICIAN;
      for (Direction dir : directions) {
        if (rc.canBuildRobot(toBuild, dir, 50)) {
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
    Team enemy = rc.getTeam().opponent();
    int actionRadius = rc.getType().actionRadiusSquared;
    int sensorRadius = rc.getType().sensorRadiusSquared;

    RobotInfo[] sensibleEnemies = rc.senseNearbyRobots(sensorRadius, enemy);
    RobotInfo[] attackableEnemies = rc.senseNearbyRobots(actionRadius, enemy);
    RobotInfo[] team = rc.senseNearbyRobots(sensorRadius, rc.getTeam());

    int flagnum = rc.getFlag(rc.getID());
    Direction direction = randomDirection();

    // If spots muckraker near enemy EC, calc flag number that directs
    // politician to enemy EC and update flag number
    if (flagnum != 0) direction = directions[flagnum];
    else {
      for (RobotInfo robot : team) {
        int otherflagnum = rc.getFlag(robot.getID());
        if (robot.getType() == RobotType.MUCKRAKER && otherflagnum != 0) {
          Direction dirToOther = rc.getLocation().directionTo(robot.location);
          flagnum = calcDirUsingMuck(otherflagnum, dirToOther);
          if (rc.canSetFlag(flagnum)) rc.setFlag(flagnum);
          direction = directions[flagnum];
          break;
        }
      }
    }

    // If spots enemy enlightenment center, move towards it
    for (RobotInfo robot : sensibleEnemies) {
      MapLocation enemyLoc = robot.getLocation();
      if (robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
        direction = rc.getLocation().directionTo(enemyLoc);
      }
    }

    // Attacks enemy enlightenment center if close enough and able to do so
    for (RobotInfo robot : attackableEnemies) {
      MapLocation enemyLoc = robot.getLocation();
      if (rc.canEmpower(actionRadius) &&
      robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
        System.out.println(rc.getID() + ": ROUND " + turnCount + " ATTACK");
        rc.empower(actionRadius);
      }
    }

    if (!tryMove(direction)) tryMove(randomDirection());
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
          System.out.println("e x p o s e d");
          rc.expose(robot.location);
          return;
        }
      }
    }
    //Check all nearby robots
    for (RobotInfo robot : rc.senseNearbyRobots(-1)) {
      if (robot.getTeam() == ally && robot.getType() == RobotType.MUCKRAKER && rc.canGetFlag(robot.getID()) && rc.getFlag(robot.getID()) != 0) {
        break;
      }
      else if (robot.getTeam() == enemy && robot.getType() == RobotType.ENLIGHTENMENT_CENTER) {
        MapLocation enemyCenter = robot.getLocation();
        int flagnum = flagnumFromDir(rc.getLocation().directionTo(enemyCenter));
        if (rc.canSetFlag(flagnum)) rc.setFlag(flagnum);
      }
      //if your flag is 0, you can move, otherwise don't
      if (rc.getFlag(rc.getID()) == 0 && tryMove(randomDirection())){
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



  public static void bug(MapLocation target){
      try{
        basicBug(target);
      } catch (GameActionException e){
        e.printStackTrace();
      }
  }

  static final double passabilityThreshold = 0.7;
  static Direction bugDirection = null;

  static void basicBug(MapLocation target) throws GameActionException{
    Direction d = rc.getLocation().directionTo(target);
    if(rc.getLocation().equals(target)){
      //do something
    }
    else{
      if(rc.isReady()){
        if(rc.canMove(d) && rc.sensePassability(rc.getLocation().add(d)) >= passabilityThreshold){
          rc.move(d);
          bugDirection = null;
        }
        else{
          if(bugDirection == null){
            bugDirection = d.rotateRight();
          }
          for (int i = 0; i < 8; i++){
            if(rc.canMove(bugDirection) && rc.sensePassability(rc.getLocation().add(bugDirection)) >= passabilityThreshold){
              rc.move(bugDirection);
              break;
            }
            bugDirection = bugDirection.rotateRight();
          }
          bugDirection = bugDirection.rotateLeft();
        }
      }
    }
  }

  static boolean tryMove(Direction dir) throws GameActionException {
    //System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
    if (rc.canMove(dir)) {
      rc.move(dir);
      return true;
    } else return false;
  }

  // Pass in dir, sets flag to number based on index of dir in directions[]
  static int flagnumFromDir(Direction dir) throws GameActionException {
    return java.util.Arrays.asList(directions).indexOf(dir);
  }

  // Calculates path to move based on a triangle formed by two
  // vertices: direction from a robot to Muck, direction from Muck to enemy EC
  static int calcDirUsingMuck(int otherflagnum, Direction toMuck) throws GameActionException {
    if (directions[otherflagnum] == toMuck) return otherflagnum;
    int flagnum = (Math.min(otherflagnum, flagnumFromDir(toMuck)) + 1) % 8;
    return flagnum;
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
}

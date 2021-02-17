package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

import static java.util.Collections.swap;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyWorm currentWorm;
    private List<Cell> gameCells;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.gameCells = getCells();
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms)
                .filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst()
                .get();
    }

    /* Yang ori mulai dari sini */

    public Command run() {
        for (int buffer = 0; buffer < 10000000; buffer++) {
            int i = (int) Math.floor(Math.sqrt(buffer+1));
        }

        if (adaBananaBomb(currentWorm)) {
            Position lempar = greedyLemparan(currentWorm.bananaBombs.range, currentWorm.bananaBombs.damageRadius, currentWorm.bananaBombs.damage, 2);
            int dist = (int) Math.round(jarak(currentWorm.position.x, currentWorm.position.y, lempar.x, lempar.y));
            if (dist > 0) {
                if (isDalamRangeLemparan(currentWorm.position.x, currentWorm.position.y, lempar.x, lempar.y, currentWorm.bananaBombs.range)) {
                    // System.out.println("LEMPAR BANANA DONG");
                    return new BananaCommand(lempar.x, lempar.y);
                }
            }
        } else if (adaSnowball(currentWorm)) {
            Position lempar = greedyLemparan(currentWorm.snowballs.range, currentWorm.snowballs.freezeRadius, 0, 3);
            int dist = (int) Math.round(jarak(currentWorm.position.x, currentWorm.position.y, lempar.x, lempar.y));
            if (dist > 0) {
                if (isDalamRangeLemparan(currentWorm.position.x, currentWorm.position.y, lempar.x, lempar.y, currentWorm.snowballs.range)) {
                    // System.out.println("LEMPAR SNOWBALL DONG");
                    return new SnowballCommand(lempar.x, lempar.y);
                }
            }
        }

        Worm target = cariMusuhTerdekatGlobal();
        if (bisaDitembak(currentWorm.position.x, currentWorm.position.y, target.position.x, target.position.y)) {
            Direction direction = resolveDirection(currentWorm.position, target.position);
            return new ShootCommand(direction);
        } else {
            target = cariTembakTerdekat();
            if (target != null) {
                Direction direction = resolveDirection(currentWorm.position, target.position);
                return new ShootCommand(direction);
            }
        }

        target = cariMusuhTerdekatGlobal();
        return digAndMoveTo(getNearestShootingPosition(target));
    }

    private double jarak(int x1, int y1, int x2, int y2) {
        int deltaX = x1-x2;
        int deltaY = y1-y2;
        return pythagoras(deltaX, deltaY);
    }

    private double pythagoras(int deltaX, int deltaY) {
        return Math.sqrt(deltaX*deltaX+deltaY*deltaY);
    }

    private boolean isValidCell(int x, int y) {
        return (x >= 0 && x < gameState.mapSize && y >= 0 && y < gameState.mapSize);
    }

    private boolean isDalamRangeTembakan(int wormX, int wormY, int deltaX, int deltaY, int range) {
        return (isValidCell(wormX+deltaX, wormY+deltaY) && !(deltaX == 0 && deltaY == 0) && Math.floor(pythagoras(deltaX, deltaY)) <= range);
    }

    private boolean isDalamRangeLemparan(int wormX, int wormY, int targetX, int targetY, int range) {
        return (isValidCell(targetX, targetY) && !(targetX == wormX && targetY == wormY) && Math.floor(jarak(wormX, wormY, targetX, targetY)) <= range);
    }

    private List<Cell> dalamRangeTembakan(int range) {
        List<Cell> cellTembakan = new ArrayList<>();
        int worm_x = currentWorm.position.x;
        int worm_y = currentWorm.position.y;

        for (int i = 1; i <= range; i++) {
            for (int deltaY = -1; deltaY <= 1; deltaY++) {
                for (int deltaX = -1; deltaX <= 1; deltaX++) {
                    if (isDalamRangeTembakan(worm_x, worm_y, deltaX*i, deltaY*i, range)) {
                        int cell_x = worm_x + deltaX;
                        int cell_y = worm_y + deltaY;
                        cellTembakan.add(gameState.map[cell_y][cell_x]);
                    }
                }
            }
        }

        return cellTembakan;
    }

    private List<Cell> dalamRangeLemparan(int range) {
        List<Cell> cellLemparan = new ArrayList<>();
        int worm_x = currentWorm.position.x;
        int worm_y = currentWorm.position.y;

        for (int j = worm_y-range; j <= worm_y+range; j++) {
            for (int i = worm_x-range; i <= worm_x+range; i++) {
                if (isDalamRangeLemparan(worm_x, worm_y, i, j, range)) {
                    cellLemparan.add(gameState.map[j][i]);
                }
            }
        }

        return cellLemparan;
    }

    private boolean bisaDitembak(int wormX, int wormY, int targetX, int targetY) {
        int deltaX = targetX-wormX;
        int deltaY = targetY-wormY;

        if (Math.floor(pythagoras(deltaX, deltaY)) <= currentWorm.weapon.range && !(deltaX == 0 && deltaY == 0) && (deltaX == 0 || deltaY == 0 || Math.abs(deltaX) == Math.abs(deltaY))) {
            while (deltaX != 0 || deltaY != 0) {
                for (Worm wormTeman : gameState.myPlayer.worms) {
                    if (wormTeman.position.x == wormX+deltaX && wormTeman.position.y == wormY+deltaY) {
                        return false;
                    }
                }
                if (gameState.map[wormY+deltaY][wormX+deltaX].type != CellType.AIR) {
                    return false;
                } else {
                    if (deltaX > 0) {
                        deltaX -= 1;
                    } else if (deltaX < 0) {
                        deltaX += 1;
                    }
                    if (deltaY > 0) {
                        deltaY -= 1;
                    } else if (deltaY < 0) {
                        deltaY += 1;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean bisaDilempar(int targetX, int targetY) {
        return (gameState.map[targetY][targetX].type != CellType.DEEP_SPACE);
    }

    private boolean adaBananaBomb(MyWorm cworm) {
        if (cworm.id == 2) {
            return (cworm.bananaBombs.count > 0);
        } else {
            return false;
        }
    }

    private boolean adaSnowball(MyWorm cworm) {
        if (cworm.id == 3) {
            return (cworm.snowballs.count > 0);
        } else {
            return false;
        }
    }

    private Worm cariTembakTerdekat() {
        double minDist = pythagoras(gameState.mapSize, gameState.mapSize);
        Worm target = null;
        for (Worm musuh : opponent.worms) {
            double dist = jarak(currentWorm.position.x, currentWorm.position.y, musuh.position.x, musuh.position.y);
            if (dist <= minDist && musuh.health > 0) {
                if (bisaDitembak(currentWorm.position.x, currentWorm.position.y, musuh.position.x, musuh.position.y)) {
                    minDist = dist;
                    target = musuh;
                }
            }
        }

        return target;
    }

    private Worm cariTembakTerdekatGlobal() {
        double minDist = 3*pythagoras(gameState.mapSize, gameState.mapSize);
        Worm target = null;
        for (Worm musuh : opponent.worms) {
            double dist = 0;
            for (Worm teman : gameState.myPlayer.worms) {
                dist += jarak(teman.position.x, teman.position.y, musuh.position.x, musuh.position.y);
            }
            if (dist <= minDist && musuh.health > 0) {
                for (Worm teman : gameState.myPlayer.worms) {
                    if (bisaDitembak(teman.position.x, teman.position.y, musuh.position.x, musuh.position.y)) {
                        minDist = dist;
                        target = musuh;
                    }
                }
            }
        }

        return target;
    }

    private Worm cariMusuhTerdekat() {
        Worm target = opponent.worms[0];
        double minDist = pythagoras(gameState.mapSize, gameState.mapSize);
        for (Worm musuh : opponent.worms) {
            double dist = jarak(currentWorm.position.x, currentWorm.position.y, musuh.position.x, musuh.position.y);
            if (dist < minDist && musuh.health > 0) {
                minDist = dist;
                target = musuh;
            }
        }

        return target;
    }

    private Worm cariMusuhTerdekatGlobal() {
        Worm target = opponent.worms[0];
        double minDist = 3*pythagoras(gameState.mapSize, gameState.mapSize);
        for (Worm musuh : opponent.worms) {
            double dist = 0;
            for (Worm teman : gameState.myPlayer.worms) {
                dist += jarak(teman.position.x, teman.position.y, musuh.position.x, musuh.position.y);
            }
            if (dist < minDist && musuh.health > 0) {
                minDist = dist;
                target = musuh;
            }
        }

        return target;
    }

    private List<Cell> dalamRadiusLemparan(int targetX, int targetY, int radius) {
        List<Cell> cellImpact = new ArrayList<>();

        for (int j = -radius; j <= +radius; j++) {
            for (int i = -radius; i <= +radius; i++) {
                if (isDalamRadiusLemparan(i, j, radius) && isValidCell(targetX+i, targetY+j)) {
                    cellImpact.add(gameState.map[targetY+j][targetX+i]);
                }
            }
        }

        return cellImpact;
    }

    private boolean isDalamRadiusLemparan(int deltaX, int deltaY, int radius) {
        return ((Math.floor(pythagoras(deltaX, deltaY)) <= radius) && (Math.round(Math.sqrt(radius)) >= Math.round(Math.sqrt(Math.abs(deltaX)+Math.abs(deltaY)))));
    }

    private int BananaBombDmg(int deltaX, int deltaY, int damage, int radius) {
        double dist = pythagoras(deltaX, deltaY);
        return (int) Math.round(damage-dist*(damage/(radius+1)));
    }

    private boolean layakSnowball(int jmlMusuh, int jmlTeman, int add_time, int time_max, int curr_max) {
        if ((jmlMusuh > jmlTeman) && ((17*(jmlMusuh-jmlTeman)) > curr_max)) {
            return true;
        } else if ((jmlMusuh > jmlTeman) && ((17*(jmlMusuh-jmlTeman)) == curr_max)) {
            return (add_time > time_max);
        } else {
            return false;
        }
    }

    private boolean layakBanana(int totalDamage, int dirt, int friendlyFire, int jmlMusuh, int curr_max) {
        int countMusuh = 0;
        for (Worm musuh : opponent.worms) {
            if (musuh.health > 0) {
                countMusuh += 1;
            }
        }
        if (((totalDamage < 10 && jmlMusuh == 1) || gameState.currentRound < 60 || (countMusuh-jmlMusuh) > 1) && !dalamBahaya(currentWorm)) {
            return false;
        } else {
            return (2*(totalDamage+dirt-friendlyFire) > curr_max);
        }
    }

    private boolean dalamBahaya(MyWorm cworm) {
        return cworm.health < 40;
    }

    private Position greedyLemparan(int range, int radius, int damage, int id) {
        Position bestPos = new Position(currentWorm.position.x, currentWorm.position.y);
        int maxPts = 0;
        int maxTime = 0;

        /* GREEDY VERSION 1
        List<Cell> cellLemparan = dalamRangeLemparan(range);
        for (Cell target : cellLemparan) {
            List<Cell> cellImpact = dalamRadiusLemparan(target.x, target.y, radius);
            int freezeTeman = 0;
            int penaltyDmg = 0;
            int freezeMusuh = 0;
            int attackDmg = 0;
            int countDirt = 0;
            for (Cell impact : cellImpact) {
                if (gameState.map[impact.y][impact.x].type == CellType.DIRT) {
                    countDirt += 1;
                } else if (bisaDilempar(impact.x, impact.y)) {
                    for (Worm musuh : opponent.worms) {
                        if (musuh.health > 0 && musuh.position.x == impact.x && musuh.position.y == impact.y) {
                            if (musuh.roundsUntilUnfrozen <= 1) {
                                freezeMusuh += 1;
                            }
                            attackDmg += BananaBombDmg(target.x-impact.x,target.y-impact.y, damage, radius);
                            if (attackDmg > musuh.health) {
                                attackDmg += 20;
                            }
                        }
                    }
                    for (Worm teman : gameState.myPlayer.worms) {
                        if (teman.health > 0 && teman.position.x == target.x && teman.position.y == target.y) {
                            freezeTeman += 1;
                            penaltyDmg += BananaBombDmg(target.x-impact.x,target.y-impact.y, damage, radius);
                            if (penaltyDmg > teman.health) {
                                penaltyDmg += 20;
                            }
                        }
                    }
                }
            }
            if (id == 3) {
                if (layakSnowball(freezeMusuh, freezeTeman, maxPts)) {
                    maxPts = 17*(freezeMusuh-freezeTeman);
                    bestPos.x = target.x;
                    bestPos.y = target.y;
                }
            } else if (id == 2) {
                if (layakBanana(attackDmg, countDirt, penaltyDmg, countMusuh, maxPts)) {
                    maxPts = 2*(attackDmg+countDirt-penaltyDmg);
                    bestPos.x = target.x;
                    bestPos.y = target.y;
                }
            }
        } */

        /* GREEDY VERSION 2 */

        for (Worm target : opponent.worms) {
            if (target.health > 0) {
                int freezeTeman = 0;
                int penaltyDmg = 0;
                int freezeMusuh = 0;
                int countMusuh = 0;
                int attackDmg = 0;
                int countDirt = 0;
                int freezeTime = 0;
                List<Cell> cellImpact = dalamRadiusLemparan(target.position.x, target.position.y, radius);
                for (Cell impact : cellImpact) {
                    if (gameState.map[impact.y][impact.x].type == CellType.DIRT) {
                        countDirt += 1;
                    } else if (bisaDilempar(impact.x, impact.y)) {
                        for (Worm musuh : opponent.worms) {
                            if (musuh.health > 0 && musuh.position.x == impact.x && musuh.position.y == impact.y) {
                                if (musuh.roundsUntilUnfrozen <= 1) {
                                    freezeMusuh += 1;
                                }
                                freezeTime += 5-musuh.roundsUntilUnfrozen;
                                countMusuh += 1;
                                attackDmg += BananaBombDmg(target.position.x-impact.x,target.position.y-impact.y, damage, radius);
                                if (attackDmg > musuh.health) {
                                    attackDmg += 20;
                                }
                            }
                        }
                        for (Worm teman : gameState.myPlayer.worms) {
                            if (teman.health > 0 && teman.position.x == target.position.x && teman.position.y == target.position.y) {
                                freezeTeman += 1;
                                penaltyDmg += BananaBombDmg(target.position.x-impact.x,target.position.y-impact.y, damage, radius);
                                if (penaltyDmg > teman.health) {
                                    penaltyDmg += 20;
                                }
                            }
                        }
                    }
                }
                if (id == 3) {
                    if (layakSnowball(freezeMusuh, freezeTeman, maxPts, freezeTime, maxTime)) {
                        maxPts = 17*(freezeMusuh-freezeTeman);
                        maxTime = freezeTime;
                        bestPos.x = target.position.x;
                        bestPos.y = target.position.y;
                        // System.out.println("LAYAK DONG DI (" + bestPos.x +", " + bestPos.y +"): " + maxPts +"/" + maxTime);
                    }
                } else if (id == 2) {
                    if (layakBanana(attackDmg, countDirt, penaltyDmg, countMusuh, maxPts)) {
                        maxPts = 2*(attackDmg+countDirt-penaltyDmg);
                        bestPos.x = target.position.x;
                        bestPos.y = target.position.y;
                    }
                }
            }
        }

        return bestPos;
    }

    private Position followWorm(Worm target) {
        Cell dest = getCellFromCoordinate(target.position.x, target.position.y);
        Cell destination = shortestPath(dest);
        int goToX = destination.x;
        int goToY = destination.y;
        return new Position(goToX, goToY);
    }

    private Position jalanKe(int targetX, int targetY) {
        Cell target = getCellFromCoordinate(targetX, targetY);
        Cell destination = shortestPath(target);
        int wormX = destination.x;
        int wormY = destination.y;
        return new Position(wormX, wormY);
    }

    /* Yang copy mentah dari Bot.java */

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if (i != x && j != y && isValidCell(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    /* Tambahan dari Alif */
    /* Cek keberadaan lava */
    private boolean lavaAdjacent(Cell block) {
        return (block.type == CellType.LAVA);
    }
    /* Cek arah yang dituju kosong / ada tanah */
    private boolean airAdjacent(Cell block) {
        return (block.type == CellType.AIR);
    }

    /* Cek ada dirt di depan */
    private boolean dirtAdjacent(Cell block) {
        return (block.type == CellType.DIRT);
    }

    /* Cek profesi musuh terdekat */
    //private int nearestEnemyProfession() {
      //  int[] jarak = new int[3];
        //int jarak1 = euclideanDistance(currentWorm.position.x,currentWorm.position.y,);
        // Implementasi tambahan nanti, masih kesulitan disini
    //}

    /* Cek musuh terdekat masih frozen */
    // Setelah dipikir-pikir, ini unapplicable karena
    // ga ada state yg menyimpan kondisi musuh beku atau nggak
    
    /* Tambahan kharisma */
    private List<Cell> getCells() {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = 0; i <= 32; i++) {
            for (int j = 0; j <= 32; j++) {
                cells.add(gameState.map[j][i]);
            }
        }

        return cells;
    }

    private double doubleEuclideanDistance(int aX, int aY, int bX, int bY) {
        return (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int)(Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private Cell getCellFromCoordinate(int x, int y) {
        return gameState.map[y][x];
    }

    private Cell shortestPath(Cell dest) {
        int dest_x = dest.x;
        int dest_y = dest.y;
        int worm_x = currentWorm.position.x;
        int worm_y = currentWorm.position.y;
        int deltaX = 0;
        int deltaY = 0;

        if (dest_x > worm_x) {
            deltaX += 1;
        } else if (dest_x < worm_x) {
            deltaX -= 1;
        }
        if (dest_y > worm_y) {
            deltaY += 1;
        } else if (dest_y < worm_y) {
            deltaY -= 1;
        }

        Cell nextSimple = gameState.map[worm_y+deltaY][worm_x+deltaX];
        Cell next = nextSimple;
        Cell next2 = next;

        if (next.type != CellType.AIR || next.occupier != null) {
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    int new_x = deltaX+i;
                    int new_y = deltaY+j;
                    if ((i*i+j*j < 2) && Math.round(pythagoras(new_x, new_y)) <= 1 && !(new_x == 0 && new_y == 0)) {
                        if (gameState.map[worm_y+new_y][worm_x+new_x].type == CellType.AIR && gameState.map[worm_y+new_y][worm_x+new_x].occupier == null) {
                            // System.out.println("WORM ID " + currentWorm.id + " MELIPIR KE (" + new_x + ", " + new_y + ")");
                            next = gameState.map[worm_y+new_y][worm_x+new_x];
                        }
                    }
                }
            }
        }

        int i = 0;
        while (i < 5 && !(next.x == next2.x && next.y == next2.y)) {
            next2 = shortestPathRun(next2, dest);
            i++;
        }
        if (i == 5) {
            return next;
        } else {
            return nextSimple;
        }
    }

    private Cell shortestPathRun(Cell asal, Cell dest) {
        int dest_x = dest.x;
        int dest_y = dest.y;
        int worm_x = asal.x;
        int worm_y = asal.y;
        int deltaX = 0;
        int deltaY = 0;

        if (dest_x > worm_x) {
            deltaX += 1;
        } else if (dest_x < worm_x) {
            deltaX -= 1;
        }
        if (dest_y > worm_y) {
            deltaY += 1;
        } else if (dest_y < worm_y) {
            deltaY -= 1;
        }

        Cell next = gameState.map[worm_y+deltaY][worm_x+deltaX];

        if (next.type == CellType.AIR && next.occupier == null) {
            return next;
        } else {
            for (int j = -1; j <= 1; j++) {
                for (int i = -1; i <= 1; i++) {
                    int new_x = deltaX+i;
                    int new_y = deltaY+j;
                    if (Math.round(pythagoras(new_x, new_y)) <= 1 && !(new_x == 0 && new_y == 0)) {
                        if (gameState.map[worm_y+new_y][worm_x+new_x].type == CellType.AIR && gameState.map[worm_y+new_y][worm_x+new_x].occupier == null) {
                            return gameState.map[worm_y+new_y][worm_x+new_x];
                        }
                    }
                }
            }

            return next;
        }
    }

    private Command digAndMoveTo(Cell dest) {
        if (dest == null) {
            return new DoNothingCommand();
        }
        Cell path = shortestPath(dest);
        if (path.type == CellType.DIRT) {
            return new DigCommand(path.x, path.y);
        } else {
            return new MoveCommand(path.x, path.y);
        }
    }

    private boolean isPowerupExist() {
        return gameCells.stream().anyMatch(c -> c.powerUp != null);
    }

    private Command moveToPowerUp() {
        boolean sorted = false;
        ArrayList<Double> distance = new ArrayList<>();
        List<Cell> dest = gameCells.stream().filter(c -> c.powerUp != null).collect(Collectors.toList());
        for (Cell d:dest) {
            distance.add(doubleEuclideanDistance(currentWorm.position.x, currentWorm.position.y, d.x, d.y));
        }
        while(!sorted) {
            sorted = true;
            for (int i = 0; i < dest.size() - 1; i++) {
                if (distance.get(i) > distance.get(i+1)) {
                    swap(distance, i, i + 1);
                    swap(dest, i, i + 1);
                    sorted = false;
                }
            }
        }

        return digAndMoveTo(dest.get(0));
    }

    private List<Cell> getShootingPosition(Worm target) {
        ArrayList<ArrayList<Cell>> cells = new ArrayList<ArrayList<Cell>>();
        ArrayList<Position> dir = new ArrayList<>();
        for (int x = -1; x <= 1 ; x++) {
            for (int y = -1; y <= 1 ; y++) {
                if (x == 0 && y == 0) {
                } else {
                    Position pos = new Position(x,y);
                    dir.add(pos);
                }
            }
        }

        for (Position direction: dir) {
            ArrayList<Cell> cell_candidate = new ArrayList<>();
            int position_x = target.position.x + direction.x;
            int position_y = target.position.y + direction.y;
            boolean occupied = false;

            while (doubleEuclideanDistance(position_x, position_y, target.position.x, target.position.y) <= 3
                    && isValidCell(position_x, position_y)
                    && !occupied) {
                if (getCellFromCoordinate(position_x, position_y).occupier == null || getCellFromCoordinate(position_x, position_y).occupier == currentWorm) {
                    if (isValidCell(position_x, position_y) && bisaDitembak(target.position.x, target.position.y, position_x, position_y)) {
                        cell_candidate.add(getCellFromCoordinate(position_x, position_y));
                    }
                } else {
                    occupied = true;
                }

                position_x += direction.x;
                position_y += direction.y;
            }

            if (!occupied) {
                cells.add(cell_candidate);
            }

        }

        List<Cell> flat_cells = cells.stream().flatMap(Collection::stream).collect(Collectors.toList());
        return flat_cells;
    }

    private Cell getNearestShootingPosition(Worm target) {
        boolean sorted = false;
        ArrayList<Double> distance = new ArrayList<>();
        List<Cell> cells = getShootingPosition(target);
        if (cells.size() == 0) {
            return null;
        }
        for (Cell cell:cells) {
            distance.add(doubleEuclideanDistance(currentWorm.position.x, currentWorm.position.y, cell.x, cell.y));
        }
        while(!sorted) {
            sorted = true;
            for (int i = 0; i < cells.size() - 1; i++) {
                if (distance.get(i) > distance.get(i+1)) {
                    swap(distance, i, i + 1);
                    swap(cells, i, i + 1);
                    sorted = false;
                }
            }
        }

        return cells.get(0);
    }
}

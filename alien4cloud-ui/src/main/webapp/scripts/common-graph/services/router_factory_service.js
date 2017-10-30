// Service that performs routing of relationships connections on the canvas.
define(function (require) {
  'use strict';

  var modules = require('modules');

  modules.get('a4c-common-graph').factory('routerFactoryService', function() {
    var directions = {
      up: 0,
      right: 1,
      down: 2,
      left: 3
    };

    // find intersection point between segments [ab] and [cd]
    function intersectSegments(a, b, c, d) {
      var xAmC = a.x - c.x;
      var yAmC = a.y - c.y;
      var xDmC = d.x - c.x;
      var yDmC = d.y - c.y;
      var xBmA = b.x - a.x;
      var yBmA = b.y - a.y;

      var divFactor = xBmA * yDmC - yBmA * xDmC;
      var r = (yAmC * xDmC - xAmC * yDmC) / divFactor;
      var s = (yAmC * xBmA - xAmC * yBmA) / divFactor;

      if (0 <= r && r <= 1 && 0 <= s && s <= 1) { // there is an intersection
        var xI = a.x + r * (b.x - a.x);
        var yI = a.y + r * (b.y - a.y);
        return {x: xI, y: yI};
      }
      return null;
    }

    /**
    * Checks if the other point has same coordinates of the current point.
    *
    * @param other The point we want to compare to the current one.
    */
    function pointEquals(p1, p2) {
      return p1.x === p2.x && p1.y === p2.y;
    }

    var ROUTER = function (bbox, gridSpacing) {
      this.lastBbox = null;
      this.gridSpacing = gridSpacing;
      this.obstacleWeight = 100000;

      // initialize the grid
      this.bbox = bbox.cloneWithPadding(this.gridSpacing * 4);
      // dimensions of the grid as number of cells
      this.gridWidth = Math.floor(this.bbox.width() / this.gridSpacing);
      this.gridHeight = Math.floor(this.bbox.height() / this.gridSpacing);

      // now create the cell array
      this.cells = [];
      this.obstacles = [];
      var i, j;
      for(i = 0; i < this.gridWidth; i++) {
        var line = [];
        for(j = 0; j < this.gridHeight; j++) {
          // push a new cell with 0 weight (empty).
          line.push({
            'weight': 0,
            'visited': 0
          });
        }
        this.cells.push(line);
      }
      this.verticalWays = [];
      this.horizontalWays = [];
      for(i = 0; i < this.gridWidth; i++) {
        this.verticalWays.push(true);
      }
      for(i = 0; i < this.gridHeight; i++) {
        this.horizontalWays.push(true);
      }
    };

    ROUTER.prototype = {
      constructor: ROUTER,
      /**
      * Add a new obstacle that routes cannot cross.
      *
      * @param bbox The bounding box of the obstacle.
      */
      addObstacle: function(bbox) {
        this.obstacles.push(bbox);
        var topLeft = this.getCellCoordinates({x: bbox.minX, y: bbox.minY});
        var bottomRight = this.getCellCoordinates({x: bbox.maxX, y: bbox.maxY});
        // iterate over target cells to add obstacle weight
        for(var i = topLeft.x; i <= bottomRight.x; i++) {
          this.verticalWays[i] = false;
          for(var j = topLeft.y; j <= bottomRight.y; j++) {
            this.cells[i][j].weight = this.obstacleWeight;
            this.horizontalWays[j] = false;
          }
        }
      },

      /**
      * Checks if the segment from p1 to p2 crosses some obstacles.
      *
      * @param p1 first point of the segment.
      * @param p2 second point of the segment.
      * @param direction from p1 to p2.
      */
      isCrossingObstacle: function(p1, p2, direction) {
        var p = p1;
        var movedToFree = false;
        while(!pointEquals(p, p2)) {
          this.cells[p.x][p.y].visited = 1;
          if(movedToFree && this.cells[p.x][p.y].weight >= this.obstacleWeight) {
            return true;
          }
          movedToFree = true;
          p = this.move(p, direction);
        }
        return false;
      },

      route: function(p1, dir1, p2, dir2) {
        var route;
        var gp1 = this.getCellCoordinates(p1); // grid p1
        var gp2 = this.getCellCoordinates(p2); // grid p2

        if(dir1 % 2 !== dir2 %2) {
          // directions are not parallel, simplest route is a direct crossing
          var gp12 = this.getRayLimit(gp1, dir1);
          var gp22 = this.getRayLimit(gp2, dir2);
          var intersection = intersectSegments(gp1, gp12, gp2, gp22);
          if(intersection !== null) { // there is an intersection
            // check that segments doesn't cross obstacles.
            if(!this.isCrossingObstacle(gp1, intersection, dir1) && !this.isCrossingObstacle(gp2, intersection, dir2)) {
              // return a simple route.
              route = [p1, this.getRealCoordinates(intersection), p2];
              this.routeCoordinatesUpdate(route, p1, dir1, p2, dir2);
              return route;
            }
          }
        } else {
          // parallel directions we have to find free ways
          var way1 = this.findNextFreeWay(gp1, dir1);
          var way2 = this.findNextFreeWay(gp2, dir2);
          var mergedWay = this.mergeWays(way1, way2, dir1);
          var nextPoint1, nextPoint2;
          if(mergedWay === null) {
            // these 2 ways cannot be merged so we should find a perpendicular way that connects them going in the p1 to p2 direction on the way.
            var directions = this.findNextDirections(gp1, gp2, dir1);
            nextPoint1 = this.pointFromWay(gp1, way1, dir1);
            nextPoint2 = this.pointFromWay(gp2, way2, dir2);
            // var linkWay = this.findNextFreeWay(nextPoint1, directions.dir1);
            var linkWay = this.findNextWay(nextPoint1, nextPoint2, way1, way2);
            var nextPoint11 = this.pointFromWay(nextPoint1, linkWay, directions.dir1);
            var nextPoint21 = this.pointFromWay(nextPoint2, linkWay, directions.dir2);

            // compute flexion points (only when same direction)
            var sourceDirections = this.directions(nextPoint1, nextPoint11);
            var targetDirections = this.directions(nextPoint2, nextPoint21);
            route = [p1, this.getRealCoordinates(nextPoint1), this.getRealCoordinates(nextPoint11),
              this.getRealCoordinates(nextPoint21), this.getRealCoordinates(nextPoint2), p2];
            if(sourceDirections.vertical === targetDirections.vertical) { // TODO this is specific for vertical loop so alien specific..
              // add vertical inflextion to the route
              var y;
              if(route[2].y-route[1].y > 0) {
                y = route[2].y - 20;
                route[2].y += 20;
              } else {
                y = route[2].y + 20;
                route[2].y -= 20;
              }
              var before2 = {
                x: route[2].x,
                y: y
              };
              if(route[3].y-route[4].y >0) {
                y = route[3].y - 20;
                route[3].y += 20;
              } else {
                y = route[3].y + 20;
                route[3].y -= 20;
              }
              var after3 = {
                x: route[3].x,
                y: y
              };
              route = [route[0], route[1], before2, route[2], route[3], after3, route[4], route[5]];
            } else {
              // TODO should add inflextion points also actually to avoid crossind nodes...
            }
            this.routeCoordinatesUpdate(route, p1, dir1, p2, dir2);
            return route;
          } else {
            // we have a common way to use so just return the route.
            nextPoint1 = this.getRealCoordinates(this.pointFromWay(gp1, mergedWay, dir1));
            nextPoint2 = this.getRealCoordinates(this.pointFromWay(gp2, mergedWay, dir2));
            route = [p1, nextPoint1, nextPoint2, p2];
            this.routeCoordinatesUpdate(route, p1, dir1, p2, dir2);
            return route;
          }
        }
        // we didn't managed to find simple route, let's use manhattan ?
        return [p1, p2];
      },

      // find a line that never cross any element.
      findNextFreeWay: function(p, direction) {
        var ways, coordinateGetter;
        if(direction % 2 === directions.up % 2) {
          ways = this.horizontalWays;
          coordinateGetter = function(point) {return point.y;};
        } else {
          ways = this.verticalWays;
          coordinateGetter = function(point) {return point.x;};
        }
        var pTemp = p;
        while(!ways[coordinateGetter(pTemp)]) {
          pTemp = this.move(pTemp, direction);
        }
        return coordinateGetter(pTemp);
      },

      // find the next segment that connects
      findNextWay: function(sourcePoint, targetPoint, sourceWay,  targetWay) {
        var src = {
          x: sourcePoint.x,
          y: sourcePoint.y
        };
        if(src.x === sourceWay) {
          var target = {
            x: targetWay,
            y: src.y
          };
          var dirs = this.directions(sourcePoint, targetPoint);
          var mover;
          if(dirs.vertical === directions.up) {
            mover = function(point) {point.y++;};
          } else {
            mover = function(point) {point.y--;};
          }
          mover(src);
          mover(target);
          while(this.isCrossingObstacle(src, target, dirs.horizontal)) {
            mover(src);
            mover(target);
          }
          return src.y;
        } else {
          // TODO, not used in current a4c rendering
        }
      },

      directions: function(sourcePoint, targetPoint) {
        var horizontalDir = sourcePoint.x < targetPoint.x ? directions.right : directions.left;
        var verticalDir = sourcePoint.y < targetPoint.y ? directions.up : directions.down;
        return {
          horizontal: horizontalDir,
          vertical: verticalDir
        };
      },

      findNextDirections: function(p1, p2, direction) {
        if(direction % 2 === directions.up % 2) {
          if(p1.x === p2.x) {
            return {
              dir1: directions.right,
              dir2: directions.right
            };
          }
          if(p1.x < p2.x) {
            return {
              dir1: directions.right,
              dir2: directions.left
            };
          }
          return {
            dir1: directions.left,
            dir2: directions.right
          };
        }
        if(p1.y === p2.y) {
          return {
            dir1: directions.up,
            dir2: directions.up
          };
        }
        if(p1.y < p2.y) {
          return {
            dir1: directions.down,
            dir2: directions.up
          };
        }
        return {
          dir1: directions.up,
          dir2: directions.down
        };
      },

      // if all ways are free from way 1 to way 2 then we should get the way in the middle as unique and shared way and then return the route.
      mergeWays: function(way1, way2, direction) {
        var i, ways;
        if(direction % 2 === directions.up % 2) {
          ways = this.horizontalWays;
        } else {
          ways = this.verticalWays;
        }

        if(way1 < way2) {
          for(i = way1; i < way2; i++) {
            if(!ways[i]) { // if a way is not free then we cannot have a common way.
              return null;
            }
          }
        } else {
          for(i = way2; i < way1; i++) {
            if(!ways[i]) { // if a way is not free then we cannot have a common way.
              return null;
            }
          }
        }

        // get the way in the middle as this is the best-looking
        return Math.ceil((way2+way1)/2);
      },

      // Get the point on the given way that is the normal project of the previous point.
      pointFromWay: function(previousPoint, way, direction) {
        if(direction%2 === directions.up) {
          return {x: previousPoint.x, y: way};
        }
        return {x: way, y: previousPoint.y};
      },

      routeCoordinatesUpdate: function(route, p1, dir1, p2, dir2) {
        var p1Target = route[1];
        if(dir1 === directions.up || dir1 === directions.down) {
          p1Target.x = p1.x;
        } else {
          p1Target.y = p1.y;
        }
        var p2Target = route[route.length-2];
        if(dir2 === directions.up || dir2 === directions.down) {
          p2Target.x = p2.x;
        } else {
          p2Target.y = p2.y;
        }
      },

      /**
      * Get the cell point that is next to the given one in the specified direction.
      *
      * @param p The point that we want to move up.
      * @param direction The direction in which to move the point.
      * @return The moved point.
      */
      move: function(p, direction) {
        var x = p.x;
        var y = p.y;
        if(direction === directions.up) {
          y--;
        } else if(direction === directions.right) {
          x++;
        } else if(direction === directions.down) {
          y++;
        } else if(direction === directions.left) {
          x--;
        }
        return {x: x, y: y};
      },

      getRayLimit: function (p, dir) {
        if(dir === directions.up) {
          return {x: p.x, y: this.bbox.minY};
        }
        if(dir === directions.right) {
          return {x: this.bbox.maxX, y: p.y};
        }
        if(dir === directions.down) {
          return {x: p.x, y: this.bbox.maxY};
        }
        if(dir === directions.left) {
          return {x: this.bbox.minX, y: p.y};
        }
      },

      /**
      * Get the real coordinates of a cell point.
      *
      * @param the point in cell coordinates.
      * @param the point in real coordinates.
      */
      getRealCoordinates: function(p) {
        var x = this.bbox.minX + this.gridSpacing * p.x;
        var y = this.bbox.minY + this.gridSpacing * p.y;
        return {x: x, y: y};
      },

      /**
      * Get the cell coordinates for a real point.
      *
      * @param the point in real coordinates.
      * @param the point in cell coordinates.
      */
      getCellCoordinates: function(p) {
        if(this.cells.length === 0) {
          return null;
        }

        var x = p.x - this.bbox.minX;
        var y = p.y - this.bbox.minY;
        var i = Math.floor(x / this.gridSpacing);
        var j = Math.floor(y / this.gridSpacing);
        if(this.cells.length < i) {
          i = this.cells.length - 1;
        }
        if(this.cells[i].length < j) {
          j = this.cells[i].length - 1;
        }

        return {x: i, y: j};
      }
    };

    return {
      directions: directions,
      create: function(bbox, gridStep) {
        return new ROUTER(bbox, gridStep);
      }
    };
  }); // factory
});// define

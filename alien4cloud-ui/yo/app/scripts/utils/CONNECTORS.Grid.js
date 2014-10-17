// Manages the weighted grid for the diagram bounding box for connector routing.
'use strict';

var CONNECTORS = {};

CONNECTORS.directions = {
  'up': 0,
  'right': 1,
  'left': 2,
  'down': 3
};

CONNECTORS.Grid = function(bbox, gridSpacing) {
  this.lastBbox = null;
  this.gridSpacing = gridSpacing;
  this.obstacleWeight = 1000;

  // initialize the grid
  this.bbox = bbox.cloneWithPadding(this.gridSpacing * 4);
  // dimensions of the grid as number of cells
  var gridWidth = Math.floor(this.bbox.width() / this.gridSpacing);
  var gridHeight = Math.floor(this.bbox.height() / this.gridSpacing);

  // now create the cell array
  this.cells = [];
  for(var i = 0; i < gridWidth; i++) {
    var line = [];
    for(var j = 0; j < gridHeight; j++) {
      // push a new cell with 0 weight (empty).
      line.push({
        'weight': 0,
        'visited': 0
      });
    }
    this.cells.push(line);
  }
};

CONNECTORS.Grid.prototype = {
  constructor: CONNECTORS.Grid,

  /*
  Update the grid based on the new bbox given as a parameter.
  */
  update: function(bbox) {
    var newBbox = bbox.cloneWithPadding(this.gridSpacing * 2);
    // compute the new grid

    // save the last computed bbox
    this.bbox = newBbox;
  },

  addObstacle: function(bbox) {
    // obstacles are bounding box.
    var topLeft = this.getCellCoordinates(new CONNECTORS.Point(bbox.minX, bbox.minY));
    var bottomRight = this.getCellCoordinates(new CONNECTORS.Point(bbox.maxX, bbox.maxY));
    // iterate over target cells to add obstacle weight
    for(var i = topLeft.x; i <= bottomRight.x; i++) {
      for(var j = topLeft.y; j <= bottomRight.y; j++) {
        this.cells[i][j].weight = this.obstacleWeight;
      }
    }
  },

  getWeight: function(start, end, point) {
    if(point === null) {
      return null;
    }
    if(point.equals(end)) {
      return Number.NEGATIVE_INFINITY;
    }
    if(point.x < 0 || point.x >= this.cells.length) {
      return 100000;
    }
    if(point.y < 0 || point.y >= this.cells[point.x].length) {
      return 100000;
    }
    return this.cells[point.x][point.y].weight + end.manhattan(point);
  },

  reverse: function(direction) {
    if(direction === CONNECTORS.directions.up) {
      return CONNECTORS.directions.down;
    }
    if(direction === CONNECTORS.directions.right) {
      return CONNECTORS.directions.left;
    }
    if(direction === CONNECTORS.directions.down) {
      return CONNECTORS.directions.up;
    }
    return CONNECTORS.directions.right;
  },

  moveCandidates: function(currentPoint, commingFrom) {
    var candidates = [];
    if(commingFrom !== CONNECTORS.directions.up) {
      candidates.push(new CONNECTORS.Point(currentPoint.x, currentPoint.y + 1));
    } else {
      candidates.push(null);
    }
    if(commingFrom !== CONNECTORS.directions.right) {
      candidates.push(new CONNECTORS.Point(currentPoint.x + 1, currentPoint.y));
    } else {
      candidates.push(null);
    }
    if(commingFrom !== CONNECTORS.directions.left) {
      candidates.push(new CONNECTORS.Point(currentPoint.x - 1, currentPoint.y));
    } else {
      candidates.push(null);
    }
    if(commingFrom !== CONNECTORS.directions.down) {
      candidates.push(new CONNECTORS.Point(currentPoint.x, currentPoint.y - 1));
    } else {
      candidates.push(null);
    }
    return candidates;
  },

  /**
  One predicate for the routing algorithm is that all obstacle are rectangle with some cells in-between them.
  This is why we can follow a main global direction and know we won't have to go back.
  */
  route: function(p1, p2) {
    var lastRouteCell, lastRoutePoint;
    // get start cell
    var start = this.getCellCoordinates(p1);
    // get target cell
    var end = this.getCellCoordinates(p2);

    var route = [p1];
    var cellRoute = [start];

    var currentCellPoint = start;
    // now let's try to build the route!
    var lastSelected = null;
    var count = 0;
    while(!currentCellPoint.equals(end) && count < 200) {
      count ++;
      var i;
      var commingFrom = lastSelected === null ? null : this.reverse(lastSelected);
      var candidates = this.moveCandidates(currentCellPoint, commingFrom);
      var weights = [];
      for(i = 0; i < candidates.length; i++) {
        weights.push(this.getWeight(start, end, candidates[i]));
      }

      var selected = 0;
      if(weights[selected] === null) {
        selected = 1;
      }
      for(i = selected+1; i < candidates.length; i++) {
        if(weights[i] !== null && weights[i] < weights[selected]) {
          selected = i;
        }
      }

      if(lastSelected !== null && selected !== lastSelected) {
        lastRouteCell = cellRoute[cellRoute.length-1];
        lastRoutePoint = route[route.length-1];

        var currentPoint = new CONNECTORS.Point(this.bbox.minX + this.gridSpacing * currentCellPoint.x, this.bbox.minY + this.gridSpacing * currentCellPoint.y);
        if(lastRouteCell.x === currentCellPoint.x) {
          route.push(new CONNECTORS.Point(lastRoutePoint.x, currentPoint.y));
        } else {
          route.push(new CONNECTORS.Point(currentPoint.x, lastRoutePoint.y));
        }
        cellRoute.push(currentCellPoint);

        // var pathCell = this.cells[currentCellPoint.x][currentCellPoint.y];
        // pathCell.visited = 1;
      }
      currentCellPoint = candidates[selected];
      var pathCell = this.cells[currentCellPoint.x][currentCellPoint.y];
      pathCell.visited = 1;
      lastSelected = selected;
    }

    lastRoutePoint = route[route.length-1];

    if(lastRoutePoint.x === currentCellPoint.x) {
      route.push(new CONNECTORS.Point(p2.x, lastRoutePoint.y));
    } else {
      route.push(new CONNECTORS.Point(lastRoutePoint.x, p2.y));
    }

    route.push(p2);

    return route;
  },

  move: function(point, direction) {
    if(direction === CONNECTORS.directions.up) {
      return new CONNECTORS.Point(point.x, point.y + 1);
    }
    if(direction === CONNECTORS.directions.down) {
      return new CONNECTORS.Point(point.x, point.y - 1);
    }
    if(direction === CONNECTORS.directions.right) {
      return new CONNECTORS.Point(point.x + 1, point.y);
    }
    return new CONNECTORS.Point(point.x - 1, point.y);
  },

  /* Get cell for a given point. */
  getCellCoordinates: function(p) {
    if(this.cells.length === 0) {
      return null;
    }

    var x = p.x - this.bbox.minX;
    var y = p.y - this.bbox.minY;
    var i = Math.floor(x / this.gridSpacing);
    var j = Math.floor(y / this.gridSpacing);
    if(this.cells.length < i) {
      i = this.cells.length;
    }
    if(this.cells[i].length < j) {
      j = this.cells[i].length;
    }

    return new CONNECTORS.Point(i, j);
  }
};

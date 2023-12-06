//Author Robert Rewcastle
//script to draw line graphs of datums with a wattage variable
//this script will put the graphs inside an html tag with a graphpoint class which
//one must put in the html document before loading the script
//requires d3 https://d3js.org/d3.v4.js

'use strict';
SolarNode.DatumCharts = (function(){
	//json field we are interested in
	var datumPropName = 'watts';
	var units = 'W';

	//This map is graphs to look up the latest reading based on their sourceId
	var datamap = {};

	// Array filter function for removing duplicate values
	function onlyUnique(value, index, self) {
		return self.indexOf(value) === index;
	}

	// used for giving the graph some white space either side of the graph
	function standardDeviation(numbersArr) {
		//--CALCULATE AVAREGE--
		var total = 0,
			datumPropName;
		for (datumPropName in numbersArr) {
			total += numbersArr[datumPropName];
		}
		var meanVal = total / numbersArr.length;
		//--CALCULATE AVAREGE--

		//--CALCULATE STANDARD DEVIATION--
		var sDprep = 0;
		for (datumPropName in numbersArr) {
			sDprep += Math.pow((parseFloat(numbersArr[datumPropName]) - meanVal), 2);
		}
		var sDresult = Math.sqrt(sDprep/numbersArr.length);
		//--CALCULATE STANDARD DEVIATION--
		return sDresult
	}

	// when a new datum comes in, this handler gets called
	// and checks if the new datum has a datumPropName value
	function handleMessage(msg) {

		var datum = JSON.parse(msg.body).data;

		//check that the datum has a reading
		if (datum[datumPropName] !== undefined) {
			$('#datum-activity-charts').removeClass('hide');

			//if we have not seen this sourceId before we need to graph it
			if (datamap[datum.sourceId] === undefined) {

				//map the sourceId to the current reading
				datamap[datum.sourceId] = datum[datumPropName];

				//add a new graph for this new sourceId
				graphinit(datum.sourceId, units);
			}

			//map the sourceId to the current reading
			datamap[datum.sourceId] = datum[datumPropName];
		}
	}

	/**
	 * Get an appropriate display unit for a given base unit and scale factor.
	 * 
	 * Use this method to render scaled data value units. Typically you would first call
	 * {@link displayScaleForValue}, passing in the largest expected value
	 * in a set of data, and then pass the result to this method to generate a display unit
	 * for the base unit for that data.
	 * 
	 * For example, given a base unit of `W` (watts) and a maximum data value of `10000`:
	 * 
	 * ```
	 * const fmt = import { * } from 'format/scale';
	 * const displayScale = fmt.displayScaleForValue(10000);
	 * const displayUnit = fmt.displayUnitForScale('W', displayScale);
	 * ```
	 * 
	 * The `displayUnit` result in that example would be `kW`.
	 *
	 * @param {string} baseUnit the base unit, for example `W` or `Wh`
	 * @param {number} scale the unit scale, which must be a recognized SI scale, such 
	 *                       as `1000` for `k`
	 * @return {string} the display unit value
	 */
	function displayUnitsForScale(baseUnit, scale) {
	  return (scale === 1000000000 ? 'G' : scale === 1000000 ? 'M' : scale === 1000 ? 'k' : '') + baseUnit;
	}

	/**
	 * Get an appropriate multiplier value for scaling a given value to a more display-friendly form.
	 * 
	 * This will return values suitable for passing to {@link displayUnitsForScale}.
	 * 
	 * @param {number} value the value to get a display scale factor for, for example the maximum value
	 *                       in a range of values
	 * @return {number} the display scale factor
	 */
	function displayScaleForValue(value) {
	  var result = 1,
	      num = Math.abs(Number(value));
	  if (isNaN(num) === false) {
	    if (num >= 1000000000) {
	      result = 1000000000;
	    } else if (num >= 1000000) {
	      result = 1000000;
	    } else if (num >= 1000) {
	      result = 1000;
	    }
	  }
	  return result;
	}
	//creates a new graph looking that plots wattage data from datums coming from source
	//this code is heavily based on the last graph from https://bost.ocks.org/mike/path/
	//apologies for the magic numbers
	function graphinit(source, units) {
		// how many sample points to have on the graph
		var n = 243,
			// time for the animation
			duration = 1000,
			
			now = new Date(Date.now() - duration),

			// a scale factor to apply to y axis labels
			displayScale = 1,

			// prefill the array with the first reading
			data = new Array(n).fill(datamap[source]);

		var margin = { top: 10, right: 0, bottom: 20, left: 60 },
			width = 320 - margin.right,
			height = 120 - margin.top - margin.bottom;

		var x = d3.scaleTime()
			.domain([now - (n - 2) * duration, now - duration])
			.range([0, width]);

		var y = d3.scaleLinear()
			.domain(calculateYDomain())
			.range([height, 0]);

		var line = d3.line()
			.curve(d3.curveStepAfter)
			.x(function (d, i) { return x(now - (n - 1 - i) * duration); })
			.y(function (d, i) { return y(d); });

		// append title and SVG chart elements inside a <div class='chart-card'>
		var container = d3.select('#datum-activity-charts-container').append('div').attr('class', 'chart-card');
		container.append('h4').text(source + ' (' + datumPropName + ')');
		var svg = container.append('svg')
			.attr('width', width + margin.left + margin.right)
			.attr('height', height + margin.top + margin.bottom)
			.attr('class', 'chart')
			.append('g')
			.attr('transform', 'translate(' + margin.left + ',' + margin.top + ')');

		svg.append('defs').append('clipPath')
			.attr('id', 'clip')
			.append('rect')
			.attr('width', width)
			.attr('height', height);

		var xAxis = d3.axisBottom().scale(x);
		
		var axisX = svg.append('g')
			.attr('class', 'x axis')
			.attr('transform', 'translate(0,' + height + ')')
			.call(xAxis);
		
		var dataAxisPrecision = 0;
		var dataAxisFormat = d3.format(',.0f');

		var yAxis = d3.axisLeft().scale(y).ticks(5).tickFormat(function(d) {
			return dataAxisFormat(d/displayScale) +' ' +displayUnitsForScale(units, displayScale);
		});

		var axisY = svg.append('g')
			.attr('class', 'y axis')
			.call(yAxis);

		var path = svg.append('g')
			.attr('clip-path', 'url(#clip)')
			.append('path')
			.datum(data)
			.attr('class', 'line')
			.attr('d', line);
		
		function calculateYDomain() {
			var yDomainPadding = 0;
			var ydomain = [d3.min(data), d3.max(data)];

			// calculate a display scale for the units, e.g. kilo, mega, giga
			displayScale = d3.max([displayScaleForValue(ydomain[0]), displayScaleForValue(ydomain[1])]);

			if ( ydomain[0] === ydomain[1] ) {
				// expand the domain so the single value is in the middle vertically
				yDomainPadding = displayScale;
			} else {
				// add a bit of padding around the edges
				yDomainPadding = standardDeviation(data.filter(onlyUnique));
			}
			ydomain[0] -= yDomainPadding;
			ydomain[1] += yDomainPadding;

			return ydomain;
		}
		
		//causes the animation of the graph to progress once called it will call itself again
		function tick() {
			//grabs the datapoint and puts it in the data array
			data.push(datamap[source]);

			// update the domains
			now = new Date();
			x.domain([now - (n - 2) * duration, now - duration]);
			y.domain(calculateYDomain());
			
			// try to display integer y-axis labels, unless range too narrow and a decimal point is required
			var newDataAxisPrecision = ((y.domain()[1]/displayScale) - (y.domain()[0]/displayScale)) < 5 ? 1 : 0;
			if ( newDataAxisPrecision !== dataAxisPrecision ) {
				dataAxisPrecision = newDataAxisPrecision;
				dataAxisFormat = d3.format(',.'+newDataAxisPrecision +'f');
			}

			// redraw y-axis (on own, fastesr transition)
			axisY.transition()
				.duration(250)
				.ease(d3.easeLinear)
				.call(yAxis);
			
			// create shared transition for x-axis and line
			var t = d3.transition()
				.duration(duration)
				.ease(d3.easeLinear);

			// redraw x-axis
			axisX.transition(t)
				.call(xAxis);

			// redraw the line
			path.datum(data)
					.attr('d', line)
					.attr('transform', null)
				.transition(t)
					.attr('transform', 'translate(' + x(now - (n - 1) * duration) + ')')
					.on('end', tick);

			// pop the old data point off the front
			data.shift();
		}

		//start animating the graph
		tick();
	}

	function subscribeAndRender(){
		//subscribe to get datums as they come, when a datum arrives it runs the handler
		var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/datum/*', null);
		SolarNode.WebSocket.subscribeToTopic(topic, handleMessage);
	}

	return Object.defineProperties(self, {
		subscribeAndRender : {value : subscribeAndRender}
	});
}());

$(document).ready(function(){
	if (!SolarNode.isAuthenticated()){
		return;
	}

	$('#datum-activity-charts').first().each(function(){
		SolarNode.DatumCharts.subscribeAndRender();
	});
});

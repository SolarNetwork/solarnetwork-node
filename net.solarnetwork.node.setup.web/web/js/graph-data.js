//Author Robert Rewcastle
//script to draw line graphs of datums with a wattage variable
//this script will put the graphs inside the graphpoint class which 
//one must put in the html document before loading the script


//json field we are interested in
var key = "watts";

//This map is graphs to look up the latest reading based on their sourceId
var datamap = {};

//this array will contain all the sourceIds of graphs
var graphs = [];

//https://stackoverflow.com/questions/1960473/get-all-unique-values-in-an-array-remove-duplicates
//This get used for deciding how many ticks on the y axis
function onlyUnique(value, index, self) { 
    return self.indexOf(value) === index;
}

//https://stackoverflow.com/questions/7343890/standard-deviation-javascript
//This get used for giving the graph some white space either side of the graph
function StandardDeviation(numbersArr) {
    //--CALCULATE AVAREGE--
    var total = 0;
    for(var key in numbersArr) 
       total += numbersArr[key];
    var meanVal = total / numbersArr.length;
    //--CALCULATE AVAREGE--
  
    //--CALCULATE STANDARD DEVIATION--
    var SDprep = 0;
    for(var key in numbersArr) 
       SDprep += Math.pow((parseFloat(numbersArr[key]) - meanVal),2);
    var SDresult = Math.sqrt(SDprep/numbersArr.length);
    //--CALCULATE STANDARD DEVIATION--
    return SDresult
    
}

//when a new datum comes in, this handler gets called
//the handler checks if the new datum has a wattage reading and 
var handler = function handleMessage(msg) {
	
	var datum = JSON.parse(msg.body).data;

	//check that the datum has a reading
	if (datum[key] != undefined){
		//map the sourceId to the current reading
		datamap[datum.sourceId] = datum[key];
		
		//if we have not seen this sourceId before we need to graph it
		if (graphs.indexOf(datum.sourceId)==-1){
			
			//add a new graph for this new sourceId
			graphs.push(datum.sourceId);
			graphinit(datum.sourceId);

		}
	}
	
	
};

//creates a new graph looking that plots wattage data from datums coming from source 
//this code is heavily based on the last graph from https://bost.ocks.org/mike/path/
function graphinit(source){


    var n = 243,//how many sample points to have on the graph
        duration = 1000,//time for the animation 
        now = new Date(Date.now() -duration),//not sure what the -duration is for

        //prefill the array with the first reading (might change in future)
        data = new Array(n).fill(datamap[source]);
  
    //positional styling for the graph
    var margin = { top: 10, right: 0, bottom: 20, left: 60 },
        width = 960 - margin.right,
        height = 120 - margin.top - margin.bottom;

    //sets the axis scales
    var x = d3.time.scale()
        .domain([now - (n - 2) * duration, now - duration])
        .range([0, width]);

    var y = d3.scale.linear()
        .range([height, 0]);
    
    //draws the line for the graph (not sure how this code works at this stage)
    var line = d3.svg.line()
        .interpolate("step-after")
        .x(function (d, i) { return x(now - (n - 1 - i) * duration); })
        .y(function (d, i) { return y(d); });

    //finds the location on the main page where graphs are to be placed and adds one
    var p = d3.select(".graphpoint").append("p").text(source + " (" + key + ")" );
    var svg = d3.select(".graphpoint").append("svg")
    .attr("width", width + margin.left + margin.right)
    .attr("height", height + margin.top + margin.bottom)
    .style("margin-left", margin.left + "px")
    .append("g")
    .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
    
    
    svg.append("defs").append("clipPath")
        .attr("id", "clip")
        .append("rect")
        .attr("width", width)
        .attr("height", height);

    var axis = svg.append("g")
        .attr("class", "x axis")
        .attr("transform", "translate(0," + height + ")")
        .call(x.axis = d3.svg.axis().scale(x).orient("bottom"));

    svg.append("g")
        .attr("class", "yaxis")
        .call(d3.svg.axis().scale(y).ticks(2).orient("left"));//the number in the ticks() determines how many steps in axis
    var path = svg.append("g")
        .attr("clip-path", "url(#clip)")
        .append("path")
        .datum(data)
        .attr("class", "line");

    var transition = d3.select({}).transition()
        .duration(duration)
        .ease("linear");

    //causes the animation of the graph to progress once called it will call itself again
    function tick() {
        transition = transition.each(function () {
        	var stddev = StandardDeviation(data);
        	var numunique = data.filter(onlyUnique).length;
            // update the domains
            now = new Date();
            x.domain([now - (n - 2) * duration, now - duration]);
            
            //if there is only 1 value of data relate it to 0 on the graph
            if (numunique == 1){
            	
            	//the number could be positive or negative, (not tested when data is 0)
            	y.domain([Math.min(0,d3.min(data)),Math.max(0,d3.max(data))]);
            }else{
            	y.domain([d3.min(data) - stddev, d3.max(data)+stddev]);
            }
            
            
            
            

            //put a data point on the graph
            data.push(datamap[source]);

            // redraw the line
            svg.select(".line")
                .attr("d", line)
                .attr("transform", null);

            // slide the x-axis left
            axis.call(x.axis);
              
            svg.select("g .yaxis")
            	//the number of ticks is the minimum of the number of unique values and 5 
                .call(d3.svg.axis().scale(y).ticks(Math.min(numunique + 1 ,5)).orient("left"));
            //y.axis.ticks(10);
            // slide the line left
            path.transition()
                .attr("transform", "translate(" + x(now - (n - 1) * duration) + ")");

            // pop the old data point off the front
            data.shift();
            
            //hack to only show 2dp accuracy probably a better way but couldn't find it
            d3.selectAll("g .yaxis").selectAll("g .tick").selectAll("text").each(function roundtick(d,i){
            	tagtext = d3.select(this).text();
            	
            	//this regex checks for positive and negative numbers that can have commas , and 0-2 decimal places
            	var re = new RegExp('-?([0-9]|,)+(\.[0-9][0-9]?)?');
            	
            	//takes the first result which should cut off the any decimal places after 2
            	d3.select(this).text(re.exec(tagtext)[0]);
            });

        }).transition().each("start", function () { tick() });
    };
    
    //start animating the graph
    tick();
}

//subscribe to get datums as they come, when a datum arrives it runs the handler
var topic = SolarNode.WebSocket.topicNameWithWildcardSuffix('/topic/datum/*', null);
SolarNode.WebSocket.subscribeToTopic(topic, handler);

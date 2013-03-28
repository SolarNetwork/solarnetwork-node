(function($) {
	$.fn.spin = function(opts, color) {
		var presets = {
			"tiny": { lines: 8, width: 2, length: 2, radius: 3, corners: 1 },
			"small": { lines: 15, width: 2, length: 5, radius: 4, corners: 1 },
			"large": { lines: 13, width: 3, length: 8, radius: 8, corners: 1 }
		};
		if (Spinner) {
			return this.each(function() {
				var $this = $(this),
					data = $this.data();
				
				if (data.spinner) {
					data.spinner.stop();
					delete data.spinner;
				}
				if (opts !== false) {
					if (typeof opts === "string") {
						if (opts in presets) {
							opts = presets[opts];
						} else {
							opts = {};
						}
						if (color) {
							opts.color = color;
						}
					}
					data.spinner = new Spinner($.extend({color: $this.css('color')}, opts)).spin(this);
				}
			});
		} else {
			throw "Spinner class not available.";
		}
	};
})(jQuery);

Highcharts.theme = {
	colors: ["#7cb5ec", "#f7a35c", "#90ee7e", "#7798BF", "#aaeeee", "#ff0066", "#eeaaee", "#55BF3B", "#DF5353", "#7798BF", "#aaeeee"],
	credits: {
		text: 'Highcharts'
	},
	chart: {
		backgroundColor: 'transparent',
		plotBackgroundColor: 'transparent',
		plotBorderWidth: 0
	},
	title: {
		style: {
			color: '#080808',
			fontSize: '22px',
			fontFamily: '"Open Sans", Calibri, Candara, Arial, sans-serif'
		}
	},
	xAxis: {
		gridLineWidth: 1,
		lineColor: '#C0C0C0',
		tickColor: '#C0C0C0',
		labels: {
			style: {
				color: '#333',
				fontSize: '12px',
				fontFamily: '"Open Sans", Calibri, Candara, Arial, sans-serif'
			}
		},
		title: {
			style: {
				color: '#333',
				fontWeight: 'bold',
				fontSize: '14px',
				fontFamily: '"Open Sans", Calibri, Candara, Arial, sans-serif'
			}
		}
	},
	yAxis: {
		minorTickInterval: 'auto',
		minorGridLineColor: '#F0F0F0',
		lineColor: '#C0C0C0',
		tickColor: '#C0C0C0',
		lineWidth: 1,
		tickWidth: 1,
		labels: {
			style: {
				color: '#000',
				fontSize: '12px',
				fontFamily: '"Open Sans", Calibri, Candara, Arial, sans-serif'
			}
		},
		title: {
			style: {
				color: '#333',
				fontWeight: 'bold',
				fontSize: '14px',
				fontFamily: '"Open Sans", Calibri, Candara, Arial, sans-serif'
			}
		}
	}
};

Highcharts.setOptions(Highcharts.theme);

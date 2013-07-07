$(document).ready(function() {
	var activeNum = Math.floor((Math.random() * $('.item').length));
	$('.item').eq(activeNum).addClass('active');
	$('.carousel-indicators li').eq(activeNum).addClass('active');
	$('.carousel').carousel({interval: 3000});
	$('.carousel').carousel('cycle');
});
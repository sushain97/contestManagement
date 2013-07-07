$(document).ready(function () {
	ChangeActive();
	$('button').on('click', function() {
		$('button').hide();
		window.print();
		$('button').show();
	});
});
	
function ChangeActive() {
	var type = $.url().param('type');
	if (type == undefined)
		$("#overview").addClass('active');
	else {
		$("#" + type).closest('.dropdown').addClass('active');
		$('#' + type).addClass('active');
		
		if(type.indexOf('middle_school_') != -1 || type.indexOf('high_school_') != -1) {
			var hash = hashCode(type.substring(type.indexOf('school_') + 7));
			$("#" + hash).closest('.dropdown').addClass('active');
			$('#' + hash).addClass('active');
		}
	}
}

hashCode = function(s) {
    var hash = 0, i, c;
    if (s.length == 0) return hash;
    for (i = 0; i < s.length; i++) {
        c = s.charCodeAt(i);
        hash = ((hash << 5) - hash) + c;
        hash = hash & hash; // Convert to 32bit integer
    }
    return hash;
};
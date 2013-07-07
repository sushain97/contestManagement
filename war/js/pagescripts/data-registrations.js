$(document).ready(function() {
	$("input:radio:checked").data("chk", true);
	$("input:radio").click(function() {
		$("input[name='" + $(this).attr("name") + "']:radio").not(this).removeData("chk");
		$(this).data("chk", !$(this).data("chk"));
		$(this).prop("checked", $(this).data("chk"));
	});
});
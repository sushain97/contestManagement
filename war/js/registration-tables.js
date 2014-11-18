$(document).ready(function() {
	$('span.addStudentBtn').click(function() {
		var numStudents = $(this).attr('data-numStudents');
		for(var i = 0; i < numStudents; i++)
			addStudent('', 6, [false, false, false, false]);
		$('#registrations').trigger('update');
	});

	$(document).on('click', 'span.deleteBtn', function() {
		var tr = $(this).parents('tr');
		tr.hide('fast', function() {
			tr.remove();
			$('#registrations').trigger('update');
			calcCost();
		});
	});

	$(document).on('click', 'span.allBtn', function() {
		$('input.testCheckbox', $(this).parents('tr')).attr("checked", true);
		calcCost();
	});

	$(document).on('click', 'span.noneBtn', function() {
		$('input.testCheckbox', $(this).parents('tr')).attr("checked", false);
		calcCost();
	});

	$(document).on('change', 'table input[type=checkbox]', function() {
		calcCost();
	});

	$(document).on('change', 'select', function() {
		$('#registrations').trigger('update');
	});

	$('button#import').click(function() {
		var students = $('textarea#importData').val().split('\n');
		var seperator = $('input#seperator').val();

		$.each(students, function() {
			var fields = this.split(seperator);
			if(fields.length == 6 && parseInt(fields[1].trim()))
				addStudent(fields[0].trim(), fields[1].trim(), [!!fields[2].trim().length, !!fields[3].trim().length, !!fields[4].trim().length, !!fields[5].trim().length]);
		});
	});

	var refreshTable = function() {
		var students = $('textarea#importData').val().split('\n');
		var seperator = $('input#seperator').val();

		var tableBody = $('table#importTable tbody');
		tableBody.empty();

		$.each(students, function() {
			var fields = this.split(seperator);
			var tr = $('<tr></tr>');

			if(fields.length < 6 || !parseInt(fields[1].trim())) {
				tr.addClass("danger");
				tr.append($('<td></td>').text('Failed'));
			}
			else {
				tr.append($('<td></td>').text(fields[0].trim()));
				tr.append($('<td></td>').text(fields[1].trim()));


				var checked = '<i class="glyphicon glyphicon-ok"></i>', unchecked = '<i class="glyphicon glyphicon-remove"></i>';
				tr.append($('<td></td>').html(fields[2].trim().length ? checked : unchecked).addClass(fields[2].trim().length ? "success" : "danger"));
				tr.append($('<td></td>').html(fields[3].trim().length ? checked : unchecked).addClass(fields[3].trim().length ? "success" : "danger"));
				tr.append($('<td></td>').html(fields[4].trim().length ? checked : unchecked).addClass(fields[4].trim().length ? "success" : "danger"));
				tr.append($('<td></td>').html(fields[5].trim().length ? checked : unchecked).addClass(fields[5].trim().length ? "success" : "danger"));
			}

			tableBody.append(tr);
		});
	};

	$('textarea#importData').on('propertychange keyup input paste', refreshTable);
	$('input#seperator').on('input', refreshTable);

	$('form').submit(function(ev) {
		var students = [];
		$.each($('.student'), function() {
			var td = $('td', this);
			students.push({
				"name": $(td[1]).find('input').val(),
				"grade": parseInt($(td[2]).find('select:visible').val()),
				"N": $(td[3]).find('input').prop('checked'),
				"C": $(td[4]).find('input').prop('checked'),
				"M": $(td[5]).find('input').prop('checked'),
				"S": $(td[6]).find('input').prop('checked')
			});
		});
		$('input[name=studentData]').remove();
		$(this).append($('<input>').attr('type', 'hidden').attr('name', 'studentData').val(JSON.stringify(students)));
	});
});

function addFrozenStudent(name, grade, subjects) {
	var tr = $('<tr class="student"></tr>');

	tr.append($('<td class="text-center"></td>').text(name));
	tr.append($('<td class="text-center"></td>').text(grade));

	for(var j = 0; j < 4; j++) {
		var td = $('<td class="text-center"></td>');
		if(subjects[j]) {
			td.html('<span class="hide">1</span><i class="glyphicon glyphicon-ok"></i>');
			td.addClass('success');
		}
		else {
			td.html('<span class="hide">0</span><i class="glyphicon glyphicon-remove"></i>');
			td.addClass('danger');
		}
		tr.append(td);
	}

	$('#registrations tbody').append(tr);
}

function addStudent(name, grade, subjects) {
	var tr = $('<tr class="student"></tr>');
	tr.append($('<td class="text-center"><span class="btn btn-xs btn-default tableBtn deleteBtn"><i class="glyphicon glyphicon-remove"></i></span></td>'));

	var td = $('<td class="text-center"></td>');
	td.append($('<input type="text" class="form-control input-sm" value required></td>').val(name));
	tr.append(td);

	var td = $('<td class="text-center"></td>');
	td.append($('<select class="elemGrades"><option value="4">4</option><option value="5">5</option>').val(grade));
	td.append($('<select class="midGrades"><option value="6">6</option><option value="7">7</option><option value="8">8</option></select>').val(grade));
	td.append($('<select class="highGrades"><option value="9">9</option><option value="10">10</option><option value="11">11</option><option value="12">12</option></select>').val(grade));
	tr.append(td);

	for(var j = 0; j < 4; j++) {
		var td = $('<td class="text-center"></td>');
		td.append($('<input type="checkbox" class="testCheckbox">').prop('checked', subjects[j]));
		tr.append(td);
	}

	tr.append('<td class="text-center visible-lg"><span class="btn btn-xs btn-default allBtn">All</span> <span class="btn btn-xs btn-default noneBtn">None</span></td>');

	$('#registrations tbody').append(tr);
	$('#registrations').trigger('update');
	calcCost();
}

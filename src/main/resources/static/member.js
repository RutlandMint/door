$(function() {

	function load() {
		$.ajax({
			url : 'self.json',
			type : "GET",
			dataType : "json"
		}).then(function(res) {
			$("#js-memberName").text(res.name);
			$("#js-memberStatus").text(res.status);

			var $access = $("#js-memberAccess");
			if (res.accessGranted) {
				$access.attr("class", "badge badge-success");
				$access.text("Granted");
				$("#js-buttonOpen").removeAttr("disabled");
			} else {
				$access.attr("class", "badge badge-danger");
				$access.text(res.denyMessage);
				$("#js-buttonOpen").attr("disabled", true);
			}

		});
	}

	$("#js-buttonOpen").attr("disabled", true);
	$("#js-buttonOpen").click(function() {
		$.ajax({
			url : 'memberOpen.do',
			type : "POST"
		}).then(load);
	});

	load();
});

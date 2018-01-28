$(function() {

	function loadAccessLog() {
		$.ajax({
			url : '/accessLog.json',
			type : "GET",
			dataType : "json"
		}).then(function(res) {
			var $lb = $("#js-logBody");
			$lb.empty();
			var _tLogRow = _.template("<tr><td><%-time%></td><td><%-name%></td><td><%-action%></td></tr>");
			_.each(res.accessLog, function(entry) {
				$lb.prepend(_tLogRow(entry));
			});
		});
	}

	function loadStatus() {
		$.ajax({
			url : '/status.json',
			type : "GET",
			dataType : "json"
		}).then(function(res) {
			if (res.accessControl.nightMode){
				$("#js-timeOfDay").attr("class", "badge badge-dark").text("Night");
			} else {
				$("#js-timeOfDay").attr("class", "badge badge-info").text("Day");
			}
			if ( res.door.status == "OK"){
				$("#js-door").attr("class", "badge badge-success")
				$("#js-door").text(formatSeconds(res.door.statusAgeSeconds));
			} else {
				$("#js-door").attr("class", "badge badge-danger")
				$("#js-door").text(res.door.status);
			}

			if (res.accessControl.memberAccessEnabled) {
				$("#js-memberAccessEnabled").attr("class", "badge badge-success").text("Enabled");
				$("#js-buttonDisable").removeAttr("disabled");
				$("#js-buttonEnable").attr("disabled", "disabled");
			} else {
				$("#js-memberAccessEnabled").attr("class", "badge badge-danger").text("Disabled");
				$("#js-buttonDisable").attr("disabled", "disabled");
				$("#js-buttonEnable").removeAttr("disabled");
			}
			if (res.database.loaded) {
				$("#js-memberCount").text(res.database.memberCount);
				if (res.database.memberCount > 10) {
					$("#js-memberCountStatus").attr("class", "badge badge-success").text("OK");
				} else if (res.database.memberCount > 0) {
					$("#js-memberCountStatus").attr("class", "badge badge-warning").text("Warning");
				} else {
					$("#js-memberCountStatus").attr("class", "badge badge-danger").text("Fail");
				}
				if (res.database.fromFile) {
					$("#js-membersLoaded").attr("class", "badge badge-warning").text("From Cache File");
				} else {
					$("#js-membersLoaded").attr("class", "badge badge-success").text("From Wild Apricot");
				}

				$("#js-memberAge").text(formatSeconds(res.database.ageSeconds));
				if (res.database.ageSeconds > 3600) {
					$("#js-memberAge").attr("class", "badge badge-danger");
				} else if (res.database.ageSeconds > 900) {
					$("#js-memberAge").attr("class", "badge badge-warning");
				} else {
					$("#js-memberAge").attr("class", "badge badge-success");
				}
			} else {
				$("#js-membersLoaded").attr("class", "badge badge-danger").text("Fail");
			}

		});
	}

	function load() {
		loadStatus();
		loadAccessLog();
	}

	$("#js-buttonDisable").click(function() {
		$.ajax({
			url : '/disable.do',
			type : "POST"
		}).then(load);
	});

	$("#js-buttonEnable").click(function() {
		$.ajax({
			url : '/enable.do',
			type : "POST"
		}).then(load);
	});

	$("#js-buttonUnlock").click(function() {
		$.ajax({
			url : '/openBriefly.do',
			type : "POST"
		}).then(load);
	});

	load();
	setInterval(loadAccessLog, 1000);
	setInterval(loadStatus, 30000);
});

function formatSeconds(seconds) {
	var seconds = Math.round(seconds);

	var negative = false;
	if (seconds < 0) {
		seconds = -1 * seconds;
		negative = true;
	}

	minutes = Math.floor(seconds / 60);
	seconds = seconds % 60;

	hours = Math.floor(minutes / 60);
	minutes = minutes % 60;

	days = Math.floor(hours / 24);
	hours = hours % 24;

	var ret = "";
	if (days) {
		ret = days + "d " + (hours ? (hours + "h ") : '') + minutes + "m";
	} else if (hours) {
		ret = hours + "h " + minutes + "m";
	} else if (minutes) {
		ret = minutes + "m " + seconds + "s";
	} else {
		ret = seconds + "s";
	}

	if (negative) {
		return "( - " + ret + ")";
	}
	return ret;
};

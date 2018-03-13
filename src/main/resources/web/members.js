$(function(){
	$.ajax({
		url : 'members.json',
		type : "GET",
		dataType : "json"
	}).then(function(res) {
		var $mb = $("#js-memberList");
		$mb.empty();
		var _tLogRow = _.template("<tr class='<%-cl%>'><td><%-name%></td><td><%-keyCardNumber%></td><td><%-level%></td><td><%-status%></td><td><%-signedWaiver%></td><td><%-signedAgreement%></td><td><%-denyMessage%></td></tr>");
		_.each(res.members, function(entry) {
			if ( entry.accessGranted ){
				entry.denyMessage = "";
				entry.cl = "";
			} else {
				entry.cl = "table-danger";
			}
			if ( !entry.keyCardNumber ){
				entry.cl = "table-warning";
				entry.denyMessage = "No Card";
			}
			entry.signedWaiver = entry.signedWaiver?"":"Missing";
			entry.signedAgreement = entry.signedAgreement?"":"Missing";
			$mb.prepend(_tLogRow(entry));
		});
		 $('#js-memberTable').DataTable({
			 "paging":   false
		 });
	});
});
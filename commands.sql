SELECT
    a.actname,
    date_trunc('minute', ag.ontime)::time AS ontime,
    date_trunc('minute', ag.endtime)::time AS endtime
FROM act a
JOIN act_gig_view ag ON a.actID = ag.actID
WHERE ag.gigID = ?
ORDER BY ag.ontime;

-- task 2
SELECT
    venueID
from venue
WHERE venueName = ?;


INSERT INTO gig (venueID, gigtitle, gigdatetime, gigstatus)
VALUES (?, ?, ?, ?)
RETURNING gigID;

INSERT INTO gig_ticket (gigID, pricetype, price, )
VALUES (?, 'A', ?);




/*Put your CREATE TABLE statements (and any other schema related definitions) here*/

DROP TABLE IF EXISTS venue CASCADE;
DROP TABLE IF EXISTS gig CASCADE;
DROP TABLE IF EXISTS act CASCADE;
DROP TABLE IF EXISTS act_gig CASCADE;
DROP TABLE IF EXISTS gig_ticket CASCADE;
DROP TABLE IF EXISTS ticket CASCADE;




CREATE TABLE venue 
(
    venueID SERIAL PRIMARY KEY,
    venuename VARCHAR(100) NOT NULL UNIQUE,
    hirecost INTEGER CHECK (hirecost > 0),
    capacity INTEGER 
);


CREATE TABLE gig 
(
    gigID SERIAL PRIMARY KEY,
    venueID INTEGER NOT NULL,
    gigtitle VARCHAR(100) NOT NULL,
    gigdatetime TIMESTAMP NOT NULL,
    gigstatus VARCHAR(1) CHECK (gigstatus IN ('G', 'C')),
    FOREIGN KEY (venueID) REFERENCES venue(venueID) ON DELETE CASCADE,
    CONSTRAINT check_gigdatetime_time CHECK 
    (
        (gigdatetime::time >= TIME '09:00' AND gigdatetime::time <= TIME '23:59')
    )
    
);



CREATE TABLE act 
(
    actID SERIAL PRIMARY KEY,
    actname VARCHAR(100) NOT NULL,
    genre VARCHAR(10),
    standardfee INTEGER CHECK (standardfee >= 0)
);


CREATE TABLE act_gig 
(
    actID INTEGER NOT NULL,
    gigID INTEGER NOT NULL,
    actgigfee INTEGER CHECK (actgigfee >= 0),
    ontime TIMESTAMP,                    
    duration INTEGER CHECK (duration >= 15 AND duration <= 90),
    PRIMARY KEY (actID, gigID, ontime),
    FOREIGN KEY (actID) REFERENCES act(actID) 
    ON DELETE CASCADE
    ON UPDATE CASCADE,
    FOREIGN KEY (gigID) REFERENCES gig(gigID) 
    ON DELETE CASCADE
    ON UPDATE CASCADE
    
);



CREATE VIEW act_gig_view AS
SELECT 
    actID,
    gigID,
    actgigfee,
    ontime,
    ontime + INTERVAL '1 minute' * duration AS endtime,
    duration
FROM
act_gig;

CREATE VIEW gig_view AS
SELECT
    g.gigID,
    g.venueID,
    g.gigtitle,
    g.gigdatetime,
    g.gigstatus,
    MAX(
        ag.ontime + INTERVAL '1 minute' * ag.duration
    ) 
    
    AS gig_endtime
FROM gig g
LEFT JOIN act_gig ag
    ON ag.gigID = g.gigID
    WHERE g.gigstatus = 'G'
GROUP BY g.gigID;

CREATE TABLE gig_ticket 
(
    gigID INTEGER NOT NULL,
    pricetype VARCHAR(2) NOT NULL,
    price INTEGER CHECK (price >= 0),
    PRIMARY KEY (gigID, pricetype),
    FOREIGN KEY (gigID) REFERENCES gig(gigID) ON DELETE CASCADE,
    CHECK((pricetype = 'A' AND price >0) OR (pricetype <> 'A' AND price >= 0))
);
CREATE TABLE ticket 
(
    ticketID SERIAL PRIMARY KEY,
    gigID INTEGER NOT NULL,
    pricetype VARCHAR(2) NOT NULL, 
    cost INTEGER,
    customername VARCHAR(100) NOT NULL,
    customeremail VARCHAR(100) NOT NULL,
    FOREIGN KEY (gigID, pricetype) 
    REFERENCES gig_ticket(gigID, pricetype) 
    ON DELETE CASCADE,
    CHECK((pricetype = 'A' AND cost >0) OR (pricetype <> 'A' AND cost >= 0))
);



CREATE OR REPLACE FUNCTION check_rule_1()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- Prevent overlapping acts in the same gig
    IF EXISTS (
        SELECT 1
        FROM act_gig_view ag
        WHERE ag.gigID = NEW.gigID
            AND ag.ontime <> NEW.ontime
            -- Overlap condition
            AND NEW.ontime < ag.endtime
            AND NEW.ontime + INTERVAL '1 minute' * NEW.duration > ag.ontime
    ) THEN 
        RAISE EXCEPTION
            'Act time overlaps with another act in this gig (gigID=%)  rule 1 ',
            NEW.gigID;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER rule_1
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_1();
    


CREATE OR REPLACE FUNCTION check_rule_2()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM act_gig_view ag
        WHERE ag.actID = NEW.actID
            AND ag.gigID <> NEW.gigID
            -- Overlap condition
            AND NEW.ontime < ag.endtime
            AND NEW.ontime + INTERVAL '1 minute' * NEW.duration > ag.ontime
    ) THEN
        RAISE EXCEPTION
            'Act is scheduled in another gig at the same time (actID=%)  rule2',
            NEW.actID;
    END IF;
    RETURN NEW;
END;
$$;


CREATE TRIGGER rule_2
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_2();

CREATE OR REPLACE FUNCTION check_rule_3()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM act_gig_view ag
        WHERE ag.actID = NEW.actID
            AND ag.gigID = NEW.gigID
            AND ag.actgigfee <> NEW.actgigfee
    ) THEN
        RAISE EXCEPTION
            'act has different prices for same total gig and dont know which one to use   (actID=%) rule 3 ',
            NEW.actID;
    END IF;
    RETURN NEW;
END;
$$;



CREATE TRIGGER rule_3
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_3();


CREATE OR REPLACE FUNCTION check_rule_4()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM act_gig ag
        WHERE ag.actID = NEW.actID
          AND ag.gigID = NEW.gigID
          AND ag.ontime <> NEW.ontime
          -- previous performance ends exactly when this starts
          AND (
                ag.ontime + INTERVAL '1 minute' * ag.duration = NEW.ontime
                OR
                NEW.ontime + INTERVAL '1 minute' * NEW.duration = ag.ontime
              )
    ) THEN
        RAISE EXCEPTION
            'Same act cannot perform back-to-back without a break (actID=% gigID=%)',
            NEW.actID, NEW.gigID;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER rule_4_no_back_to_back
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_4();


CREATE OR REPLACE FUNCTION check_rule_5()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM act_gig ag
        WHERE ag.actID = NEW.actID
            AND ag.gigID <> NEW.gigID
            AND ag.ctid <> NEW.ctid
            -- same day
            AND DATE(ag.ontime) = DATE(NEW.ontime)
            -- travel-time constraint (60 minutes)
            AND NEW.ontime <
                ag.ontime + INTERVAL '1 minute' * ag.duration + INTERVAL '60 minutes'
            AND NEW.ontime + INTERVAL '1 minute' * NEW.duration >
                ag.ontime - INTERVAL '60 minutes'
    ) THEN
        RAISE EXCEPTION
            'act must have at least 60 minutes travel time between gigs on the same day (actID=%)   rule 5',
            NEW.actID;
    END IF;
    RETURN NEW;
END;
$$;



CREATE TRIGGER rule_5   
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_5();






CREATE OR REPLACE FUNCTION check_rule_7()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    prev_end TIMESTAMP;
    next_start TIMESTAMP;
    gap INTERVAL;
BEGIN
    -- Previous act end
    SELECT ag.ontime + INTERVAL '1 minute' * ag.duration
    INTO prev_end
    FROM act_gig ag
    WHERE ag.gigID = NEW.gigID
      AND ag.ontime < NEW.ontime
    ORDER BY ag.ontime DESC
    LIMIT 1;

    -- Next act start
    SELECT ag.ontime
    INTO next_start
    FROM act_gig ag
    WHERE ag.gigID = NEW.gigID
      AND ag.ontime > NEW.ontime
    ORDER BY ag.ontime
    LIMIT 1;

    -- Check gap before NEW
    IF prev_end IS NOT NULL THEN
        gap := NEW.ontime - prev_end;
        IF gap < INTERVAL '10 minutes' OR gap > INTERVAL '30 minutes' THEN
            RAISE EXCEPTION
                'Interval after act must be between 10 and 30 minutes (gap=%) (act1 = %), (act2 = %)',
                gap, NEW.ontime,prev_end;
        END IF;
    END IF;

    -- Check gap after NEW
    IF next_start IS NOT NULL THEN
        gap := next_start - (NEW.ontime + INTERVAL '1 minute' * NEW.duration);
        IF gap < INTERVAL '10 minutes' OR gap > INTERVAL '30 minutes' THEN
            RAISE EXCEPTION
                'Interval after act must be between 10 and 30 minutes (gap=%) (act1 = %), (act2 = %)',
                gap, NEW.ontime,next_start;
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER rule_7_intervals
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_7();




CREATE OR REPLACE FUNCTION check_rule_8()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    earliest_time TIMESTAMP;
    gig_start     TIMESTAMP;
BEGIN
    -- Get the gig start time
    SELECT g.gigdatetime
    INTO gig_start
    FROM gig g
    WHERE g.gigID = NEW.gigID 
    AND g.gigstatus = 'G';

    -- Find the earliest act time for this gig
    SELECT MIN(ag.ontime)
    INTO earliest_time
    FROM act_gig ag
    WHERE ag.gigID = NEW.gigID;

    -- Determine what the earliest time WOULD be after this insert/update
    IF earliest_time IS NULL OR NEW.ontime < earliest_time THEN
        earliest_time := NEW.ontime;
    END IF;

    -- If this act is the first act, it must start at gig start time
    IF earliest_time = NEW.ontime AND NEW.ontime <> gig_start THEN
        RAISE EXCEPTION
            'First act must start at the gig start time (gigID=%)',
            NEW.gigID;
    END IF;
    RETURN NEW;
END;
$$;



CREATE TRIGGER rule_8
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_8();



CREATE OR REPLACE FUNCTION check_rule_9()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    tickets_sold INTEGER;
    venue_capacity INTEGER;
BEGIN
    -- Count tickets sold for this gig
    SELECT COUNT(*)
    INTO tickets_sold
    FROM ticket t
    WHERE t.gigID = NEW.gigID;

    -- Get venue capacity
    SELECT v.capacity
    INTO venue_capacity
    FROM gig g
    JOIN venue v ON g.venueID = v.venueID
    WHERE g.gigID = NEW.gigID
    AND g.gigstatus = 'G';

    -- Block if capacity exceeded
    IF tickets_sold + 1 > venue_capacity THEN
        RAISE EXCEPTION
            'Venue capacity exceeded for gigID=%',
            NEW.gigID;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER rule_9
BEFORE INSERT
ON ticket
FOR EACH ROW
EXECUTE FUNCTION check_rule_9();



CREATE OR REPLACE FUNCTION check_rule_11()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    gig_end TIMESTAMP;
    has_loud_genre BOOLEAN;
BEGIN
    -- Get gig end time
    SELECT g.gig_endtime
    INTO gig_end
    FROM gig_view g
    WHERE g.gigID = NEW.gigID
    AND g.gigstatus = 'G';

    -- Check if gig involves Rock or Pop
    SELECT EXISTS (
        SELECT 1
        FROM act_gig ag
        JOIN act a ON a.actID = ag.actID
        WHERE ag.gigID = NEW.gigID
          AND a.genre IN ('Rock', 'Pop')
    )
    INTO has_loud_genre;
    IF gig_end IS NULL THEN
        return NEW;
    END IF;

    -- Apply curfew rules
    IF has_loud_genre THEN
        IF gig_end::time > TIME '23:00' THEN
            RAISE EXCEPTION
                'Gigs involving Rock or Pop must finish by 23:00';
        END IF;
    ELSE
        IF gig_end::time > TIME '01:00' AND gig_end::time < TIME '09:00' THEN
            RAISE EXCEPTION
                'Gigs must finish by 01:00';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER rule_11
BEFORE INSERT OR UPDATE
ON act_gig
FOR EACH ROW
EXECUTE FUNCTION check_rule_11();








/*
1. There must be no overlap between acts at a gig (although one act can start as soon as the previous act has finished, e.g. it's fine if act 1 finishes at 20:30 and act 2 starts at 20:30)
Do this by getting all acts at a gig sort them by date time then binary search for the first item that starts before the start of this act and calculate the end time and look forward to make sure no  acts end while it does.
2. Acts cannot perform in multiple gigs at the same time
get all act gig with the act 

*/
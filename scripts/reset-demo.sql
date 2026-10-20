BEGIN;

DELETE FROM seat_holds;

UPDATE flight_seats
SET status = 'AVAILABLE'
WHERE flight_code = 'VN123';

COMMIT;
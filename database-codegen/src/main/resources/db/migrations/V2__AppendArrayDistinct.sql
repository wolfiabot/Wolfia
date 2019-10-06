-- Merge two arrays, but only keep distinct values.
CREATE OR REPLACE FUNCTION array_append_distinct(array1 anyarray, array2 anyarray)
    RETURNS anyarray AS
$$
SELECT ARRAY(SELECT unnest(array1) UNION SELECT unnest(array2))
$$ LANGUAGE SQL IMMUTABLE;



-- Remove array2 elements from array1
CREATE OR REPLACE FUNCTION array_diff(array1 anyarray, array2 anyarray)
    RETURNS anyarray AS
$$
SELECT COALESCE(array_agg(elem), '{}')
FROM unnest(array1) elem
WHERE elem <> ALL (array2)
$$ LANGUAGE SQL IMMUTABLE;

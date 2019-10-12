ALTER TABLE public.setup
    ALTER COLUMN game DROP NOT NULL;

UPDATE public.setup
SET game = NULL
WHERE game = 'POPCORN';

ALTER TABLE public.setup
    ALTER COLUMN mode DROP NOT NULL;

UPDATE public.setup
SET mode = NULL
WHERE mode = 'WILD';

ALTER TABLE public.setup
    ALTER COLUMN day_length DROP NOT NULL;
ALTER TABLE public.setup
    ALTER COLUMN day_length DROP DEFAULT;

-- consolidate the two former defaults
UPDATE public.setup
SET day_length = 300000
WHERE day_length = 600000;

UPDATE public.setup
SET day_length = NULL
WHERE day_length = 300000;

DELETE
FROM public.setup
WHERE game IS NULL
  AND mode IS NULL
  AND day_length IS NULL
  AND inned_users = '{}';


ALTER TABLE public.setup
    RENAME TO game_setup;

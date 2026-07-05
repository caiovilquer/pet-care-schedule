-- Rastreia quantas vezes um evento recorrente já se repetiu, permitindo que
-- Recurrence.hasNext(executeCount, lastDate) saiba quando a série (repetitions
-- ou finalDate) se esgotou. Sem isso, Event.complete() não teria como decidir
-- se deve gerar a próxima ocorrência.
ALTER TABLE event ADD COLUMN occurrence_count INTEGER NOT NULL DEFAULT 0;

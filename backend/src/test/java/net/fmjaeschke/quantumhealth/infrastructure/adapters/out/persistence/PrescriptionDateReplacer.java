package net.fmjaeschke.quantumhealth.infrastructure.adapters.out.persistence;

import com.github.database.rider.core.replacers.Replacer;
import org.dbunit.dataset.ReplacementDataSet;

import java.time.OffsetDateTime;

/**
 * Relative-date replacers for prescription fixture data.
 * Values are deliberately at safe margin around the 30-day threshold:
 * THIRTY_ONE_DAYS_AGO is just over, ONE_DAY_AGO is well under.
 * Do not "tidy" these to round numbers — boundary proximity causes flakes.
 */
public class PrescriptionDateReplacer implements Replacer {

    @Override
    public void addReplacements(ReplacementDataSet dataSet) {
        dataSet.addReplacementSubstring("[DAY,THIRTY_ONE_DAYS_AGO]",
                OffsetDateTime.now().minusDays(31).toString());
        dataSet.addReplacementSubstring("[DAY,ONE_DAY_AGO]",
                OffsetDateTime.now().minusDays(1).toString());
    }
}

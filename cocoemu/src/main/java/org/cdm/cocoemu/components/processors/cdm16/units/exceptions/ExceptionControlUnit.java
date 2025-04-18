package org.cdm.cocoemu.components.processors.cdm16.units.exceptions;

import org.cdm.cocoemu.components.processors.cdm16.Cdm16;

public class ExceptionControlUnit {
    public static ExceptionControlUnitOutputParameters compute(ExceptionControlUnitInputParameters parameters) {
        boolean gotException = true;

        int excNumber = getExceptionNumber(parameters);

        if (excNumber == -1) {
            gotException = false;
        }

        boolean unrecoverableInstruction = parameters.rtiInstruction() || parameters.intInstruction();

        boolean internalExceptionHappened =
                parameters.exc_trig_sp() || parameters.exc_trig_pc() || parameters.exc_trig_invalid_inst();

        boolean multipleInstructionsOnSamePhase =
                internalExceptionHappened && parameters.exc_trig_ext();

        boolean multipleInstructionsAcrossPhases =
                (internalExceptionHappened || parameters.exc_trig_ext()) && parameters.exc_latch();

        boolean latch_double_fault =
                unrecoverableInstruction
                        || multipleInstructionsOnSamePhase
                        || multipleInstructionsAcrossPhases;

        int exc_internal_vec_reg_output;

        if (latch_double_fault) {
            exc_internal_vec_reg_output = Cdm16.ExceptionNumbers.DOUBLE_FAULT;
        } else {
            exc_internal_vec_reg_output = excNumber;
        }

        boolean exc_internal_vec_reg_en = gotException;

        boolean exc_triggered = gotException || parameters.exc_latch();

        boolean exc_latch_output = gotException;

        boolean critical_fault = gotException && parameters.double_fault();

        boolean interruptGranted = parameters.int_en() && parameters.irq();

        boolean int_ack = interruptGranted && parameters.fetch();

        boolean latch_int = interruptGranted || exc_triggered;

        boolean reset_exc = exc_triggered && parameters.fetch();

        return new ExceptionControlUnitOutputParameters(
                exc_internal_vec_reg_output,
                exc_internal_vec_reg_en,
                exc_triggered,
                critical_fault,
                exc_latch_output,
                int_ack,
                latch_int,
                reset_exc,
                latch_double_fault
        );
    }

    private static int getExceptionNumber(ExceptionControlUnitInputParameters parameters) {
        int triggeredExceptionsMask = 0;

        if (parameters.exc_trig_sp()) {
            triggeredExceptionsMask |= 1 << Cdm16.ExceptionNumbers.UNALIGNED_SP;
        }
        if (parameters.exc_trig_pc()) {
            triggeredExceptionsMask |= 1 << Cdm16.ExceptionNumbers.UNALIGNED_PC;
        }
        if (parameters.exc_trig_invalid_inst()) {
            triggeredExceptionsMask |= 1 << Cdm16.ExceptionNumbers.INVALID_INST;
        }
        if (parameters.exc_trig_ext()) {
            triggeredExceptionsMask |= 1 << Cdm16.ExceptionNumbers.EXTERNAL_EXC;
        }

        return Integer.SIZE - Integer.numberOfLeadingZeros(triggeredExceptionsMask) - 1;
    }
}

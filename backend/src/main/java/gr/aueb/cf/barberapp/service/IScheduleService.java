package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.TimeOffInsertDTO;
import gr.aueb.cf.barberapp.dto.TimeOffReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.WorkingHoursDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// The barber's weekly template and one-off absences.
public interface IScheduleService {

    List<WorkingHoursDTO> getWorkingHours();

    // Replaces the whole weekly template in one call.
    List<WorkingHoursDTO> replaceWorkingHours(List<WorkingHoursDTO> blocks)
            throws EntityInvalidArgumentException;

    List<TimeOffReadOnlyDTO> getTimeOff(LocalDate from, LocalDate to);

    TimeOffReadOnlyDTO addTimeOff(TimeOffInsertDTO dto) throws EntityInvalidArgumentException;

    TimeOffReadOnlyDTO deleteTimeOff(UUID uuid) throws EntityNotFoundException;
}

package gr.aueb.cf.barberapp.service;

import gr.aueb.cf.barberapp.core.exceptions.EntityAlreadyExistsException;
import gr.aueb.cf.barberapp.core.exceptions.EntityInvalidArgumentException;
import gr.aueb.cf.barberapp.core.exceptions.EntityNotFoundException;
import gr.aueb.cf.barberapp.dto.UserInsertDTO;
import gr.aueb.cf.barberapp.dto.UserReadOnlyDTO;
import gr.aueb.cf.barberapp.dto.UserUpdateDTO;

public interface IUserService {

    // Public self-registration. Always creates a CUSTOMER with a matching Customer profile.
    UserReadOnlyDTO register(UserInsertDTO dto)
            throws EntityAlreadyExistsException, EntityInvalidArgumentException;

    UserReadOnlyDTO getByUsername(String username) throws EntityNotFoundException;

    UserReadOnlyDTO updateOwnProfile(String username, UserUpdateDTO dto)
            throws EntityNotFoundException, EntityAlreadyExistsException;
}

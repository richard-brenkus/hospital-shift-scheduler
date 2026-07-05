package com.richardbrenkus.shiftschedulermodernized.repository;

import com.richardbrenkus.shiftschedulermodernized.entity.ShiftRequest;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ShiftRequestRepository extends CrudRepository<ShiftRequest, Long> {

    @Query("SELECT r FROM ShiftRequest r WHERE r.shiftRequestId = :id")
    ShiftRequest getShiftRequestByID(@Param("id") Long id);

}

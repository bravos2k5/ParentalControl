package com.bravos.parentalcontrol.repository;

import com.bravos.parentalcontrol.model.Session;
import lombok.NonNull;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends CrudRepository<@NonNull Session, @NonNull String> {


  List<Session> findByDeviceId(String deviceId);

  @NonNull
  List<Session> findAll();

}

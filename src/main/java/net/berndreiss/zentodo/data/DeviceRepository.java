package net.berndreiss.zentodo.data;

import net.berndreiss.zentodo.data.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByDeviceIdUserId(long id);
}

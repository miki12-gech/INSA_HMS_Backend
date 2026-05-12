package com.hospital.hms.service;

import com.hospital.hms.entity.Holiday;
import com.hospital.hms.entity.TimeSchedule;
import com.hospital.hms.entity.TimeSlot;
import com.hospital.hms.repository.HolidayRepository;
import com.hospital.hms.repository.TimeScheduleRepository;
import com.hospital.hms.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final TimeScheduleRepository timeScheduleRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final HolidayRepository holidayRepository;

    private static final String[] ALL_SLOT;

    static {
        ALL_SLOT = new String[288];
        int k = 0;
        // The legacy system had specific typos and a strange AM/PM split. 
        // We replicate it programmatically closely mirroring the legacy array's text outputs.
        // It starts at "12:00 PM", then "12:05 AM" till 11:55 AM, then "12:00 AM", then "12:05 PM" till 11:55 PM.
        // As seen in legacy lines 78-366
        ALL_SLOT[0] = "12:00 PM";
        for (int i = 1; i <= 143; i++) {
            ALL_SLOT[i] = formatTimeLegacy(i * 5, "AM");
            // Legacy typo: index 8 is "12:40 PM" instead of AM
            if (i == 8) ALL_SLOT[i] = "12:40 PM";
        }
        ALL_SLOT[144] = "12:00 AM";
        for (int i = 145; i < 288; i++) {
            ALL_SLOT[i] = formatTimeLegacy((i - 144) * 5, "PM");
        }
    }

    private static String formatTimeLegacy(int totalMinutes, String amPm) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours == 0) hours = 12;
        return String.format("%02d:%02d %s", hours, mins, amPm);
    }

    public List<TimeSchedule> getSchedules() {
        return timeScheduleRepository.findAll();
    }

    public List<TimeSchedule> getScheduleByDoctor(String doctor) {
        return timeScheduleRepository.findByDoctor(doctor);
    }

    @Transactional
    public void addSchedule(TimeSchedule schedule) throws Exception {
        int key1 = getSlotIndex(schedule.getSTime());
        int key2 = getSlotIndex(schedule.getETime());

        if (key1 == -1 || key2 == -1) {
            throw new Exception("Time Selection Error! Invalid format.");
        }
        if (key1 > key2) {
            throw new Exception("Time Selection Error!");
        }

        List<TimeSchedule> previousTime = timeScheduleRepository.findByDoctorAndWeekday(schedule.getDoctor(), schedule.getWeekday());

        if (!previousTime.isEmpty()) {
            for (TimeSchedule preTime : previousTime) {
                // If ID matches, we are editing, we skip it
                if (schedule.getId() != null && schedule.getId().equals(preTime.getId())) {
                    continue;
                }
                int keyPreS = getSlotIndex(preTime.getSTime());
                int keyPreE = getSlotIndex(preTime.getETime());

                if (key1 < keyPreS) {
                    if (key2 > keyPreS) {
                        throw new Exception("Slot Overlapped!");
                    }
                } else if (key1 > keyPreS) {
                    if (key1 < keyPreE) {
                        throw new Exception("Slot Overlapped!");
                    }
                } else if (key1 >= keyPreS && key2 <= keyPreE) {
                    throw new Exception("Slot Overlapped!");
                } else if (key1 == keyPreS) {
                    throw new Exception("Slot Overlapped!");
                }
            }
        }

        schedule.setSTimeKey(String.valueOf(key1));
        
        if (schedule.getId() != null) {
            timeScheduleRepository.save(schedule);
        } else {
            int duration = Integer.parseInt(schedule.getDuration());
            int slotSTime = key1;
            int slotETime = key1 + duration;

            while (slotSTime < key2 && slotETime <= key2) {
                String pSlotSTime = ALL_SLOT[slotSTime];
                String pSlotETime = ALL_SLOT[slotETime];

                TimeSlot slot = new TimeSlot();
                slot.setDoctor(schedule.getDoctor());
                slot.setSTime(pSlotSTime);
                slot.setETime(pSlotETime);
                slot.setWeekday(schedule.getWeekday());
                slot.setSTimeKey(String.valueOf(slotSTime));
                slot.setHospitalId(schedule.getHospitalId());
                timeSlotRepository.save(slot);

                slotSTime = slotETime;
                slotETime = slotSTime + duration;
            }
            timeScheduleRepository.save(schedule);
        }
    }

    @Transactional
    public void deleteSchedule(Long id, String doctor, String weekday) {
        timeSlotRepository.deleteByDoctorAndWeekday(doctor, weekday);
        timeScheduleRepository.deleteById(id);
    }
    
    public List<TimeSlot> getTimeSlotsByDoctor(String doctor) {
         return timeSlotRepository.findByDoctorOrderBySTimeKeyAsc(doctor);
    }

    public List<Holiday> getHolidays() {
        return holidayRepository.findAll();
    }

    public List<Holiday> getHolidaysByDoctor(String doctor) {
        return holidayRepository.findByDoctorOrderByDateAsc(doctor);
    }

    public void addHoliday(Holiday holiday) {
        holidayRepository.save(holiday);
    }

    public void deleteHoliday(Long id) {
        holidayRepository.deleteById(id);
    }

    private int getSlotIndex(String time) {
        for (int i = 0; i < ALL_SLOT.length; i++) {
            if (ALL_SLOT[i].equals(time)) {
                return i;
            }
        }
        return -1;
    }
}

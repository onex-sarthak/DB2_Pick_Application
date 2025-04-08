package org.onextel.db2_pick_app.service.dlr;

import org.onextel.db2_pick_app.dto.DlrCallbackRequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class DlrCallbackBuffer {

    private final List<DlrCallbackRequestDto> buffer = Collections.synchronizedList(new ArrayList<>());

    public boolean add(DlrCallbackRequestDto dto, int maxSize) {
        synchronized (buffer) {
            buffer.add(dto);
            return buffer.size() >= maxSize;
        }
    }

    public List<DlrCallbackRequestDto> drain() {
        synchronized (buffer) {
            List<DlrCallbackRequestDto> drained = new ArrayList<>(buffer);
            buffer.clear();
            return drained;
        }
    }

    public boolean isEmpty() {
        return buffer.isEmpty();
    }

    public int size() {
        return buffer.size();
    }
}

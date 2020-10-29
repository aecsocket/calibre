package me.aecsocket.calibre.item.component.search;

import me.aecsocket.calibre.item.component.CalibreComponentSlot;

public class SlotSearchOptions {
    private String slotTag;
    private Integer targetPriority;

    public SlotSearchOptions() {}
    public SlotSearchOptions(SlotSearchOptions o) {
        slotTag = o.slotTag;
        targetPriority = o.targetPriority;
    }

    public String getSlotTag() { return slotTag; }
    public SlotSearchOptions slotTag(String slotTag) { this.slotTag = slotTag; return this; }

    public Integer getTargetPriority() { return targetPriority; }
    public SlotSearchOptions targetPriority(Integer targetPriority) { this.targetPriority = targetPriority; return this; }

    public boolean matches(CalibreComponentSlot slot) {
        if (slotTag != null && slot.getTags() != null && !slot.getTags().contains(slotTag)) return false;
        if (targetPriority != null && slot.getPriority() != targetPriority) return false;
        return true;
    }
}

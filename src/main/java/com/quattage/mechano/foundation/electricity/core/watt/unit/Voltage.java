package com.quattage.mechano.foundation.electricity.core.watt.unit;

/**
 * A Voltage object stores voltage backed by an unsigned short.
 * This measure inherently makes voltage values safer to handle, as 
 * implementations fundamentally cannot make use of invalid voltage values.
 * 
 * This also has a smaller footprint than what would otherwise be required, which
 * comes in handy for sending packets or writing to NBT.
 * 
 * Legal voltage values fall in multiples of 4 whole numbers ranging from <code>0 - 262,140</code>.
 * 
 * @see {@link WattUnitConversions#toStoredVolts(int) WattUnitConversions} 
 *        for more info on how voltage is converted.
 */
public class Voltage {

    public static final Voltage MAX_VOLTS = new Voltage(Short.MAX_VALUE);

    private short volts;

    public Voltage(int volts) {
        this.volts = WattUnitConversions.toStoredVolts(volts);
    }

    public Voltage(short volts) {
        this.volts = volts;
    }

    public int get() {
        return WattUnitConversions.toRealVolts(volts);
    }

    public void setTo(int volts) {
        this.volts = WattUnitConversions.toStoredVolts(volts);
    }

    public boolean equals(Object other) {
        if(!(other instanceof Voltage v)) return false;
        return this.volts == v.volts;
    }

    public int hashCode() {
        return (int)volts;
    }

    public short getRaw() {
        return volts;
    }
}

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

    private Voltage(Voltage v) {
        this.volts = v.getRaw();
    }

    public Voltage copy() {
        return new Voltage(this);
    }

    /**
     * @param other Voltage to compare
     * @return The greater voltage between <code>this</code> and <code>other</code>. 
     * If both voltages are equal, <code>other</code> is returned by default.
     */
    public Voltage getGreater(Voltage other) {
        if(this.isGreaterThan(other)) return this;
        return other;
    }

    public boolean isGreaterThan(Voltage that) {
        return this.getRaw() > that.getRaw();
    }

    public int get() {
        return WattUnitConversions.toRealVolts(volts);
    }

    public void setTo(int volts) {
        this.volts = WattUnitConversions.toStoredVolts(volts);
    }

    public void setTo(Voltage volts) {
        this.volts = volts.getRaw();
    }

    public boolean equals(Object other) {
        if(!(other instanceof Voltage v)) return false;
        return this.volts == v.volts;
    }

    public int hashCode() {
        return (int)volts;
    }

    public String toString() {
        return "" + get();
    }

    public short getRaw() {
        return volts;
    }
}

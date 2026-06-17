class EnergyDataMock {
    static energyData = [
        { customerRef: "XXX", consumptionDate: new Date(2024, 1, 1), consumptionAmount: 12 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 2, 1), consumptionAmount: 13 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 3, 1), consumptionAmount: 8 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 4, 1), consumptionAmount: 5 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 5, 1), consumptionAmount: 2 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 6, 1), consumptionAmount: 2 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 7, 1), consumptionAmount: 2 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 8, 1), consumptionAmount: 4 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 9, 1), consumptionAmount: 7 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 10, 1), consumptionAmount: 8 },
        { customerRef: "XXX", consumptionDate: new Date(2024, 11, 1), consumptionAmount: 9 },
        { customerRef: "XXX", consumptionDate: new Date(2025, 0, 1), consumptionAmount: 14 },
    ];

    static async getEnergyConsumptionByCustomerAndDate(customerRef, date) {
        let entry = this.energyData.find(entry => 
            entry.customerRef === customerRef && 
            entry.consumptionDate.getFullYear() === date.getFullYear() &&
            entry.consumptionDate.getMonth() === date.getMonth()
        ) || null;
        if(entry === null) return null;
        return {
            externalReferenceCode: `${entry.customerRef}-${entry.consumptionDate.getFullYear()}${String(entry.consumptionDate.getMonth() + 1).padStart(2, '0')}`,
            consumptionDate: entry.consumptionDate,
            consumptionAmount: entry.consumptionAmount,
            consumptionUnit: "kWh"
        }
    }

    static async getEnergyConsumptionAtPageIndex(page, pageSize) {
        const startIndex = page * pageSize;
        return this.energyData.slice(startIndex, startIndex + pageSize).map(entry => ({
            externalReferenceCode: `${entry.customerRef}-${entry.consumptionDate.getFullYear()}${String(entry.consumptionDate.getMonth() + 1).padStart(2, '0')}`,
            consumptionDate: entry.consumptionDate,
            consumptionAmount: entry.consumptionAmount,
            consumptionUnit: "kWh"
        }));
    }

}

export default EnergyDataMock;
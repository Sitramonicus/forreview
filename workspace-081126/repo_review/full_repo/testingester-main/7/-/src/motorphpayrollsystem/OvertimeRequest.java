
package motorphpayrollsystem;

public class OvertimeRequest {

    private int employeeID;
    private double requestedHours;
    private String approvalStatus;

    public OvertimeRequest(int employeeID, double requestedHours) {
        setEmployeeID(employeeID);
        setRequestedHours(requestedHours);
        this.approvalStatus = "Pending";
    }

    public int getEmployeeID() {
        return employeeID;
    }

    public void setEmployeeID(int employeeID) {
        if (employeeID <= 0) {
            throw new IllegalArgumentException(
                    "Employee ID must be greater than 0."
            );
        }

        this.employeeID = employeeID;
    }

    public double getRequestedHours() {
        return requestedHours;
    }

    public void setRequestedHours(double requestedHours) {
        if (!Double.isFinite(requestedHours) || requestedHours <= 0) {
            throw new IllegalArgumentException(
                    "Requested overtime hours must be greater than 0."
            );
        }

        this.requestedHours = requestedHours;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        if (approvalStatus == null ||
                (!approvalStatus.equalsIgnoreCase("Pending") &&
                        !approvalStatus.equalsIgnoreCase("Approved") &&
                        !approvalStatus.equalsIgnoreCase("Rejected"))) {

            throw new IllegalArgumentException(
                    "Approval status must be Pending, Approved, or Rejected."
            );
        }

        if (approvalStatus.equalsIgnoreCase("Approved")) {
            this.approvalStatus = "Approved";
        } else if (approvalStatus.equalsIgnoreCase("Rejected")) {
            this.approvalStatus = "Rejected";
        } else {
            this.approvalStatus = "Pending";
        }
    }

    public boolean isApproved() {
        return "Approved".equals(approvalStatus);
    }
}
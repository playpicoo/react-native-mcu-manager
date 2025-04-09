import ExpoModulesCore

struct UpdateOptions: Record {
    @Field var estimatedSwapTime: TimeInterval = 0
    @Field var upgradeFileType: Int = 0
    @Field var upgradeMode: Int?
    @Field var memoryAlignment: Int?
    @Field var windowUploadCapacity: Int?
    @Field var requestConnectionPriority: Bool?
}

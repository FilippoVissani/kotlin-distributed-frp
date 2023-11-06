package io.github.filippovissani.kotlin_distributed_dfrp

typealias DeviceID = Int
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighborField<T> = Map<DeviceID, T>
typealias SensorID = String
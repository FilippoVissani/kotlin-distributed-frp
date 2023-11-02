package io.github.filippovissani.kotlin_distributed_dfrp

typealias DeviceID = Int
typealias Path = List<Slot>
typealias Export<T> = ExportTree<T>
typealias NeighbourField<T> = Map<DeviceID, T>
